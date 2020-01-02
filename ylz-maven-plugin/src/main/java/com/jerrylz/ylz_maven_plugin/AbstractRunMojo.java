/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jerrylz.ylz_maven_plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactFeatureFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;

import org.springframework.boot.loader.tools.FileUtils;
import org.springframework.boot.loader.tools.MainClassFinder;

import com.alibaba.fastjson.JSONObject;

/**
 * Base class to run a spring application.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author David Liu
 * @author Daniel Young
 * @author Dmytro Nosan
 * @since 1.3.0
 * @see RunMojo
 */
public abstract class AbstractRunMojo extends AbstractDependencyFilterMojo {

	/**
	 * springboot上下文注解
	 */
	private static final String SPRING_BOOT_APPLICATION_CLASS_NAME = "org.springframework.boot.autoconfigure.SpringBootApplication";

	/**
	 * The Maven project.
	 * maven项目
	 * @since 1.0.0
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * Add maven resources to the classpath directly, this allows live in-place editing of
	 * resources. Duplicate resources are removed from {@code target/classes} to prevent
	 * them to appear twice if {@code ClassLoader.getResources()} is called. Please
	 * consider adding {@code spring-boot-devtools} to your project instead as it provides
	 * this feature and many more.
	 * 将maven资源直接添加到类路径下,从而可以实时编辑资源.从类路径中删除重复的资源,
	 * 以防止该资源出现两次
	 * @since 1.0.0
	 */
	@Parameter(property = "spring-boot.run.addResources", defaultValue = "false")
	private boolean addResources = false;

	/**
	 * Path to agent jar. NOTE: a forked process is required to use this feature.
	 * @since 1.0.0
	 * @deprecated since 2.2.0 in favor of {@code agents}
	 */
	@Deprecated
	@Parameter(property = "spring-boot.run.agent")
	private File[] agent;

	/**
	 * Path to agent jars. NOTE: a forked process is required to use this feature.
	 * agent jars路径.注意：要使用此功能，需要forked进程
	 * @since 2.2.0
	 */
	@Parameter(property = "spring-boot.run.agents")
	private File[] agents;

	/**
	 * Flag to say that the agent requires -noverify.
	 * agent是否配置参数 -noverify
	 * @since 1.0.0
	 */
	@Parameter(property = "spring-boot.run.noverify")
	private boolean noverify = false;

	/**
	 * Current working directory to use for the application. If not specified, basedir
	 * will be used. NOTE: a forked process is required to use this feature.
	 * 当前工作目录.如果未指定则使用basedir.注意：用于forked进程
	 * @since 1.5.0
	 */
	@Parameter(property = "spring-boot.run.workingDirectory")
	private File workingDirectory;

	/**
	 * JVM arguments that should be associated with the forked process used to run the
	 * application. On command line, make sure to wrap multiple values between quotes.
	 * NOTE: a forked process is required to use this feature.
	 * JVM参数（与启动程序的分叉进程有关.）
	 * @since 1.1.0
	 */
	@Parameter(property = "spring-boot.run.jvmArguments")
	private String jvmArguments;

	/**
	 * List of JVM system properties to pass to the process. NOTE: a forked process is
	 * required to use this feature.
	 * 系统属性变量
	 * @since 2.1.0
	 */
	@Parameter
	private Map<String, String> systemPropertyVariables;

	/**
	 * List of Environment variables that should be associated with the forked process
	 * used to run the application. NOTE: a forked process is required to use this
	 * feature.
	 * 环境变量（与分叉进程启动程序有关）
	 * @since 2.1.0
	 */
	@Parameter
	private Map<String, String> environmentVariables;

	/**
	 * Arguments that should be passed to the application. On command line use commas to
	 * separate multiple arguments.
	 * 程序参数,多个参数逗号隔开
	 * @since 1.0.0
	 */
	@Parameter(property = "spring-boot.run.arguments")
	private String[] arguments;

	/**
	 * The spring profiles to activate. Convenience shortcut of specifying the
	 * 'spring.profiles.active' argument. On command line use commas to separate multiple
	 * profiles.
	 * 环境配置参数（多个配置逗号隔开）
	 * @since 1.3.0
	 */
	@Parameter(property = "spring-boot.run.profiles")
	private String[] profiles;

	/**
	 * The name of the main class. If not specified the first compiled class found that
	 * contains a 'main' method will be used.
	 * 主类名称（如果未指定,将使用找到的第一个包含“main”方法的类）
	 * @since 1.0.0
	 */
	@Parameter(property = "spring-boot.run.main-class")
	private String mainClass;

	/**
	 * Additional folders besides the classes directory that should be added to the
	 * classpath.
	 * 其他目录（除了class目录外其他目录添加到class路径）
	 * @since 1.0.0
	 */
	@Parameter(property = "spring-boot.run.folders")
	private String[] folders;

	/**
	 * Directory containing the classes and resource files that should be packaged into
	 * the archive.
	 * 类和资源文件目录
	 * @since 1.0.0
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	private File classesDirectory;

	/**
	 * Flag to indicate if the run processes should be forked. Disabling forking will
	 * disable some features such as an agent, custom JVM arguments, devtools or
	 * specifying the working directory to use.
	 * 是否分叉运行进程.禁用分叉将禁用某些功能,如代理,自定义jvm参数,热部署等
	 * @since 1.2.0
	 */
	@Parameter(property = "spring-boot.run.fork", defaultValue = "true")
	private boolean fork;

	/**
	 * Flag to include the test classpath when running.
	 * 运行时是否包含测试类路径
	 * @since 1.3.0
	 */
	@Parameter(property = "spring-boot.run.useTestClasspath", defaultValue = "false")
	private Boolean useTestClasspath;

	/**
	 * Skip the execution.
	 * 是否跳过执行
	 * @since 1.3.2
	 */
	@Parameter(property = "spring-boot.run.skip", defaultValue = "false")
	private boolean skip;

	/**
	 * 执行指令
	 * @throws MojoExecutionException
	 * @throws MojoFailureException
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		//判断是否执行
		if (this.skip) {
			getLog().debug("skipping run as per configuration.");
			return;
		}
		//运行指令
		run(getStartClass());
	}

	/**
	 * Specify if the application process should be forked.
	 * 返回fork标志
	 * @return {@code true} if the application process should be forked
	 */
	protected boolean isFork() {
		return this.fork;
	}

	/**
	 * Specify if fork should be enabled by default.
	 * @return {@code true} if fork should be enabled by default
	 * @see #logDisabledFork()
	 * @deprecated as of 2.2.0 in favour of enabling forking by default.
	 */
	@Deprecated
	protected boolean enableForkByDefault() {
		return hasAgent() || hasJvmArgs() || hasEnvVariables() || hasWorkingDirectorySet();
	}

	private boolean hasAgent() {
		File[] configuredAgents = determineAgents();
		return (configuredAgents != null && configuredAgents.length > 0);
	}

	private boolean hasJvmArgs() {
		return (this.jvmArguments != null && !this.jvmArguments.isEmpty())
				|| (this.systemPropertyVariables != null && !this.systemPropertyVariables.isEmpty());
	}

	private boolean hasEnvVariables() {
		return (this.environmentVariables != null && !this.environmentVariables.isEmpty());
	}

	private boolean hasWorkingDirectorySet() {
		return this.workingDirectory != null;
	}

	/**
	 * 运行指令
	 * @param startClassName
	 * @throws MojoExecutionException
	 * @throws MojoFailureException
	 */
	private void run(String startClassName) throws MojoExecutionException, MojoFailureException {
		//是否启用fork进程
		boolean fork = isFork();
		//设置全局属性fork启用标志
		this.project.getProperties().setProperty("_spring.boot.fork.enabled", Boolean.toString(fork));
		if (fork) {
			//启用fork进程执行
			doRunWithForkedJvm(startClassName);
		}
		else {
			logDisabledFork();
			runWithMavenJvm(startClassName, resolveApplicationArguments().asArray());
		}
	}

	/**
	 * Log a warning indicating that fork mode has been explicitly disabled while some
	 * conditions are present that require to enable it.
	 */
	protected void logDisabledFork() {
		if (getLog().isWarnEnabled()) {
			if (hasAgent()) {
				getLog().warn("Fork mode disabled, ignoring agent");
			}
			if (hasJvmArgs()) {
				RunArguments runArguments = resolveJvmArguments();
				getLog().warn("Fork mode disabled, ignoring JVM argument(s) ["
						+ String.join(" ", runArguments.asArray()) + "]");
			}
			if (hasWorkingDirectorySet()) {
				getLog().warn("Fork mode disabled, ignoring working directory configuration");
			}
		}
	}

	/**
	 * fork进程执行
	 * @param startClassName
	 * @throws MojoExecutionException
	 * @throws MojoFailureException
	 */
	private void doRunWithForkedJvm(String startClassName) throws MojoExecutionException, MojoFailureException {
		//整合命令参数列表
		List<String> args = new ArrayList<>();
		//添加agent相关参数
		addAgents(args);
		//添加jvm参数
		addJvmArgs(args);
		//添加类路径以及依赖jar路径
		addClasspath(args);
		//添加启动类名称
		args.add(startClassName);
		//添加运行参数
		addArgs(args);
		runWithForkedJvm((this.workingDirectory != null) ? this.workingDirectory : this.project.getBasedir(), args,
				determineEnvironmentVariables());
	}

	/**
	 * Run with a forked VM, using the specified command line arguments.
	 * @param workingDirectory the working directory of the forked JVM
	 * @param args the arguments (JVM arguments and application arguments)
	 * @param environmentVariables the environment variables
	 * @throws MojoExecutionException in case of MOJO execution errors
	 * @throws MojoFailureException in case of MOJO failures
	 */
	protected abstract void runWithForkedJvm(File workingDirectory, List<String> args,
			Map<String, String> environmentVariables) throws MojoExecutionException, MojoFailureException;

	/**
	 * Run with the current VM, using the specified arguments.
	 * @param startClassName the class to run
	 * @param arguments the class arguments
	 * @throws MojoExecutionException in case of MOJO execution errors
	 * @throws MojoFailureException in case of MOJO failures
	 */
	protected abstract void runWithMavenJvm(String startClassName, String... arguments)
			throws MojoExecutionException, MojoFailureException;

	/**
	 * Resolve the application arguments to use.
	 * 解析应用程序参数（如main方法参数）
	 * @return a {@link RunArguments} defining the application arguments
	 */
	protected RunArguments resolveApplicationArguments() {
		getLog().info("========="+this.arguments.length);
		//创建运行参数对象
		RunArguments runArguments = new RunArguments(this.arguments);
		//添加环境配置参数
		addActiveProfileArgument(runArguments);
		return runArguments;
	}

	/**
	 * Resolve the environment variables to use.
	 * 解析环境变量
	 * @return an {@link EnvVariables} defining the environment variables
	 */
	protected EnvVariables resolveEnvVariables() {
		return new EnvVariables(this.environmentVariables);
	}

	/**
	 * 添加运行参数
	 * @param args
	 */
	private void addArgs(List<String> args) {
		//解析运行参数
		RunArguments applicationArguments = resolveApplicationArguments();
		//将运行参数放入args队列
		Collections.addAll(args, applicationArguments.asArray());
		//打印日志
		logArguments("Application argument(s): ", this.arguments);
	}

	/**
	 *
	 * @return
	 */
	private Map<String, String> determineEnvironmentVariables() {
		//解析环境变量
		EnvVariables envVariables = resolveEnvVariables();
		logArguments("Environment variable(s): ", envVariables.asArray());
		return envVariables.asMap();
	}

	/**
	 * Resolve the JVM arguments to use.
	 * 解析JVM参数
	 * @return a {@link RunArguments} defining the JVM arguments
	 */
	protected RunArguments resolveJvmArguments() {
		StringBuilder stringBuilder = new StringBuilder();
		if (this.systemPropertyVariables != null) {
			//拼接系统变量,多个变量以空格隔开
			stringBuilder.append(this.systemPropertyVariables.entrySet().stream()
					.map((e) -> SystemPropertyFormatter.format(e.getKey(), e.getValue()))
					.collect(Collectors.joining(" ")));
		}
		if (this.jvmArguments != null) {
			//拼接JVM参数
			stringBuilder.append(" ").append(this.jvmArguments);
		}
		//返回运行参数实例
		return new RunArguments(stringBuilder.toString());
	}

	/**
	 * 添加jvm参数
	 * @param args
	 */
	private void addJvmArgs(List<String> args) {
		//解析JVM参数
		RunArguments jvmArguments = resolveJvmArguments();
		//将jvm参数添加到args
		Collections.addAll(args, jvmArguments.asArray());
		logArguments("JVM argument(s): ", jvmArguments.asArray());
	}

	/**
	 * 添加agent相关参数
	 * @param args
	 */
	private void addAgents(List<String> args) {
		//获取agent路径
		File[] configuredAgents = determineAgents();
		if (configuredAgents != null) {
			if (getLog().isInfoEnabled()) {
				getLog().info("Attaching agents: " + Arrays.asList(configuredAgents));
			}
			//configuredAgents不为空则添加命令参数
			for (File agent : configuredAgents) {
				//使用该参数可以在执行main方法前执行指定的javaagent包特定代码
				args.add("-javaagent:" + agent);
			}
		}

		if (this.noverify) {
			//关闭Java字节码校验
			args.add("-noverify");
		}
	}

	/**
	 * 获取agent路径
	 * @return
	 */
	private File[] determineAgents() {
		return (this.agents != null) ? this.agents : this.agent;
	}

	/**
	 * 添加环境配置参数
	 * @param arguments
	 */
	private void addActiveProfileArgument(RunArguments arguments) {
		//如果存在环境配置
		if (this.profiles.length > 0) {
			StringBuilder arg = new StringBuilder("--spring.profiles.active=");
			for (int i = 0; i < this.profiles.length; i++) {
				//拼接环境配置参数.多个以逗号相隔
				arg.append(this.profiles[i]);
				if (i < this.profiles.length - 1) {
					arg.append(",");
				}
			}
			//将环境配置参数放到参数队列首位
			arguments.getArgs().addFirst(arg.toString());
			//记录日志
			logArguments("Active profile(s): ", this.profiles);
		}
	}

	/**
	 * 添加类路径
	 * @param args
	 * @throws MojoExecutionException
	 */
	private void addClasspath(List<String> args) throws MojoExecutionException {
		try {
			StringBuilder classpath = new StringBuilder();
			//遍历类路径URL
			for (URL ele : getClassPathUrls()) {
				if (classpath.length() > 0) {
					//拼接路径分隔符
					classpath.append(File.pathSeparator);
				}
				//拼接路径
				classpath.append(new File(ele.toURI()));
			}
			if (getLog().isDebugEnabled()) {
				getLog().debug("Classpath for forked process: " + classpath);
			}
			//添加-cp参数
			args.add("-cp");
			//添加classes路径以及依赖jar路径
			args.add(classpath.toString());
		}
		catch (Exception ex) {
			throw new MojoExecutionException("Could not build classpath", ex);
		}
	}

	/**
	 * 获取启动类
	 * @return
	 * @throws MojoExecutionException
	 */
	private String getStartClass() throws MojoExecutionException {
		//获取配置的启动类
		String mainClass = this.mainClass;
		//如果为空未配置
		if (mainClass == null) {
			try {
				//在类路径下找@SpringBootApplication注解的类
				mainClass = MainClassFinder.findSingleMainClass(this.classesDirectory,
						SPRING_BOOT_APPLICATION_CLASS_NAME);
			}
			catch (IOException ex) {
				throw new MojoExecutionException(ex.getMessage(), ex);
			}
		}
		//没有@SpringBootApplication注解的类则抛异常
		if (mainClass == null) {
			throw new MojoExecutionException("Unable to find a suitable main class, please add a 'mainClass' property");
		}
		return mainClass ;
	}

	/**
	 * 获取类和资源文件路径
	 * @return
	 * @throws MojoExecutionException
	 */
	protected URL[] getClassPathUrls() throws MojoExecutionException {
		try {
			//封装路径
			List<URL> urls = new ArrayList<>();
			//添加用户自定义目录
			addUserDefinedFolders(urls);
			//添加资源文件路径
			addResources(urls);
			//添加classes目录
			addProjectClasses(urls);
			//添加依赖
			addDependencies(urls);
			//转化成URL数组
			return urls.toArray(new URL[0]);
		}
		catch (IOException ex) {
			throw new MojoExecutionException("Unable to build classpath", ex);
		}
	}

	/**
	 * 添加用户自定义目录
	 * @param urls
	 * @throws MalformedURLException
	 */
	private void addUserDefinedFolders(List<URL> urls) throws MalformedURLException {
		getLog().info("=============folders【"+folders.length+"】=================");
		if (this.folders != null) {
			for (String folder : this.folders) {
				//将文件转换成链接放入urls列表
				urls.add(new File(folder).toURI().toURL());
			}
		}
	}

	/**
	 * 添加资源文件路径
	 * @param urls
	 * @throws IOException
	 */
	private void addResources(List<URL> urls) throws IOException {
		//判断是否添加资源文件
		if (this.addResources) {
			//遍历项目资源文件
			for (Resource resource : this.project.getResources()) {
				File directory = new File(resource.getDirectory());
				//将资源文件转换成链接放入urls
				urls.add(directory.toURI().toURL());
				//删除classes目录中重复的文件
				FileUtils.removeDuplicatesFromOutputDirectory(this.classesDirectory, directory);
			}
		}
	}

	/**
	 * 添加classes目录
	 * @param urls
	 * @throws MalformedURLException
	 */
	private void addProjectClasses(List<URL> urls) throws MalformedURLException {
		getLog().info("==============this.classesDirectory.toURI().toURL()=========【"+this.classesDirectory.toURI().toURL()+"】");
		urls.add(this.classesDirectory.toURI().toURL());
	}

	/**
	 * 添加依赖
	 * @param urls
	 * @throws MalformedURLException
	 * @throws MojoExecutionException
	 */
	private void addDependencies(List<URL> urls) throws MalformedURLException, MojoExecutionException {
		//获取Artifacts过滤器
		FilterArtifacts filters = (this.useTestClasspath ? getFilters() : getFilters(new TestArtifactFilter()));
		//过滤项目依赖
		Set<Artifact> artifacts = filterDependencies(this.project.getArtifacts(), filters);
		//遍历依赖集合
		for (Artifact artifact : artifacts) {
			if (artifact.getFile() != null) {
				//如果依赖的jar包存在则将其转换成链接形式放入urls集合
				urls.add(artifact.getFile().toURI().toURL());
			}
		}
	}

	/**
	 * 记录Arguments日志
	 * @param message
	 * @param args
	 */
	private void logArguments(String message, String[] args) {
		if (getLog().isDebugEnabled()) {
			getLog().debug(Arrays.stream(args).collect(Collectors.joining(" ", message, "")));
		}
	}

	/**
	 * 测试过滤器
	 */
	private static class TestArtifactFilter extends AbstractArtifactFeatureFilter {

		TestArtifactFilter() {
			//跳过test
			super("", Artifact.SCOPE_TEST);
		}

		/**
		 * 获取生命周期阶段
		 * @param artifact
		 * @return
		 */
		@Override
		protected String getArtifactFeature(Artifact artifact) {
			return artifact.getScope();
		}

	}

	/**
	 * Isolated {@link ThreadGroup} to capture uncaught exceptions.
	 */
	class IsolatedThreadGroup extends ThreadGroup {

		private final Object monitor = new Object();

		private Throwable exception;

		IsolatedThreadGroup(String name) {
			super(name);
		}

		@Override
		public void uncaughtException(Thread thread, Throwable ex) {
			if (!(ex instanceof ThreadDeath)) {
				synchronized (this.monitor) {
					this.exception = (this.exception != null) ? this.exception : ex;
				}
				getLog().warn(ex);
			}
		}

		void rethrowUncaughtException() throws MojoExecutionException {
			synchronized (this.monitor) {
				if (this.exception != null) {
					throw new MojoExecutionException(
							"An exception occurred while running. " + this.exception.getMessage(), this.exception);
				}
			}
		}

	}

	/**
	 * Runner used to launch the application.
	 */
	class LaunchRunner implements Runnable {

		private final String startClassName;

		private final String[] args;

		LaunchRunner(String startClassName, String... args) {
			this.startClassName = startClassName;
			this.args = (args != null) ? args : new String[] {};
		}

		@Override
		public void run() {
			Thread thread = Thread.currentThread();
			ClassLoader classLoader = thread.getContextClassLoader();
			try {
				Class<?> loadClass = classLoader.loadClass("com.jerrylz.TestYlz");
				Method method = loadClass.getMethod("test");
				method.invoke(loadClass.newInstance());
				Class<?> startClass = classLoader.loadClass(this.startClassName);
				Method mainMethod = startClass.getMethod("main", String[].class);
				getLog().info("==============mainMethod【"+mainMethod.getName()+"】");
				if (!mainMethod.isAccessible()) {
					mainMethod.setAccessible(true);
				}
				mainMethod.invoke(null, new Object[] { this.args });
			}
			catch (NoSuchMethodException ex) {
				Exception wrappedEx = new Exception(
						"The specified mainClass doesn't contain a main method with appropriate signature.", ex);
				thread.getThreadGroup().uncaughtException(thread, wrappedEx);
			}
			catch (Exception ex) {
				thread.getThreadGroup().uncaughtException(thread, ex);
			}
		}

	}

	/**
	 * Format System properties.
	 */
	static class SystemPropertyFormatter {

		static String format(String key, String value) {
			if (key == null) {
				return "";
			}
			if (value == null || value.isEmpty()) {
				return String.format("-D%s", key);
			}
			return String.format("-D%s=\"%s\"", key, value);
		}

	}

}
