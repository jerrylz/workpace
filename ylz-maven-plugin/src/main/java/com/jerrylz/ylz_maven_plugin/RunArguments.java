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

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

import org.codehaus.plexus.util.cli.CommandLineUtils;

/**
 * Parse and expose arguments specified in a single string.
 *
 * @author Stephane Nicoll
 */
class RunArguments {

	//空字符串数组
	private static final String[] NO_ARGS = {};
	//运行参数队列
	private final Deque<String> args = new LinkedList<>();

	/**
	 * 构造运行参数对象
	 * @param arguments
	 */
	RunArguments(String arguments) {
		this(parseArgs(arguments));
	}

	/**
	 * 构造运行参数对象
	 * @param args
	 */
	RunArguments(String[] args) {
		if (args != null) {
			//args不为空则将非空值加入队列
			Arrays.stream(args).filter(Objects::nonNull).forEach(this.args::add);
		}
	}

	/**
	 * 获取运行参数队列
	 * @return
	 */
	Deque<String> getArgs() {
		return this.args;
	}

	/**
	 * 获取运行参数数组
	 * @return
	 */
	String[] asArray() {
		return this.args.toArray(new String[0]);
	}

	/**
	 * 解析参数
	 * @param arguments
	 * @return
	 */
	private static String[] parseArgs(String arguments) {
		//参数为空直接返回NO_ARGS
		if (arguments == null || arguments.trim().isEmpty()) {
			return NO_ARGS;
		}
		try {
			//去掉回车换行
			arguments = arguments.replace('\n', ' ').replace('\t', ' ');
			//解析命令行
			return CommandLineUtils.translateCommandline(arguments);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Failed to parse arguments [" + arguments + "]", ex);
		}
	}

}
