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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;

/**
 * A base mojo filtering the dependencies of the project.
 *
 * @author Stephane Nicoll
 * @author David Turanski
 * @since 1.1.0
 */
public abstract class AbstractDependencyFilterMojo extends AbstractMojo {

	/**
	 * Collection of artifact definitions to include. The {@link Include} element defines
	 * a {@code groupId} and {@code artifactId} mandatory properties and an optional
	 * 要包含的artifact集合
	 * {@code classifier} property.
	 * @since 1.2.0
	 *
	 */
	@Parameter(property = "spring-boot.includes")
	private List<Include> includes;

	/**
	 * Collection of artifact definitions to exclude. The {@link Exclude} element defines
	 * 要排除的artifact集合
	 * a {@code groupId} and {@code artifactId} mandatory properties and an optional
	 * {@code classifier} property.
	 * @since 1.1.0
	 */
	@Parameter(property = "spring-boot.excludes")
	private List<Exclude> excludes;

	/**
	 * Comma separated list of groupId names to exclude (exact match).
	 * 要排除的GroupId集合（多个值以逗号隔开并且完全匹配）
	 * @since 1.1.0
	 */
	@Parameter(property = "spring-boot.excludeGroupIds", defaultValue = "")
	private String excludeGroupIds;

	/**
	 * 设置Excludes
	 * @param excludes
	 */
	protected void setExcludes(List<Exclude> excludes) {
		this.excludes = excludes;
	}

	/**
	 * 设置Includes
	 * @param includes
	 */
	protected void setIncludes(List<Include> includes) {
		this.includes = includes;
	}

	/**
	 * 设置ExcludeGroupIds
	 * @param excludeGroupIds
	 */
	protected void setExcludeGroupIds(String excludeGroupIds) {
		this.excludeGroupIds = excludeGroupIds;
	}

	/**
	 * 过滤项目依赖
	 * @param dependencies
	 * @param filters
	 * @return
	 * @throws MojoExecutionException
	 */
	protected Set<Artifact> filterDependencies(Set<Artifact> dependencies, FilterArtifacts filters)
			throws MojoExecutionException {
		try {
			Set<Artifact> filtered = new LinkedHashSet<>(dependencies);
			//获取项目依赖与过滤后的依赖集合的交集
			filtered.retainAll(filters.filter(dependencies));
			//返回两者交集
			return filtered;
		}
		catch (ArtifactFilterException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}

	/**
	 * Return artifact filters configured for this MOJO.
	 * 获取该mojo配置的Artifacts过滤器
	 * @param additionalFilters optional additional filters to apply
	 * @return the filters
	 */
	protected final FilterArtifacts getFilters(ArtifactsFilter... additionalFilters) {
		//Artifacts过滤器
		FilterArtifacts filters = new FilterArtifacts();
		for (ArtifactsFilter additionalFilter : additionalFilters) {
			//添加额外的过滤器
			filters.addFilter(additionalFilter);
		}
		//添加排除GroupIds过滤器
		filters.addFilter(new MatchingGroupIdFilter(cleanFilterConfig(this.excludeGroupIds)));
		if (this.includes != null && !this.includes.isEmpty()) {
			//添加包含Artifacts过滤器
			filters.addFilter(new IncludeFilter(this.includes));
		}
		if (this.excludes != null && !this.excludes.isEmpty()) {
			//添加排除Artifacts过滤器
			filters.addFilter(new ExcludeFilter(this.excludes));
		}
		return filters;
	}

	/**
	 * 去除以逗号相隔的字符串前后空格
	 * @param content
	 * @return
	 */
	private String cleanFilterConfig(String content) {
		if (content == null || content.trim().isEmpty()) {
			return "";
		}
		StringBuilder cleaned = new StringBuilder();
		//解析字符串,指定分隔符','
		StringTokenizer tokenizer = new StringTokenizer(content, ",");
		//是否还有分隔符
		while (tokenizer.hasMoreElements()) {
			//拼接从当前位置到下一个分隔符的字符串
			cleaned.append(tokenizer.nextToken().trim());
			if (tokenizer.hasMoreElements()) {
				//拼接','
				cleaned.append(",");
			}
		}
		return cleaned.toString();
	}

}
