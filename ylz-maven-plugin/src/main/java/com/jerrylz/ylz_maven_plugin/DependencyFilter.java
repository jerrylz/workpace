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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;

/**
 * Base class for {@link ArtifactsFilter} based on a {@link FilterableDependency} list.
 * 依赖过滤器
 * @author Stephane Nicoll
 * @author David Turanski
 * @since 1.2.0
 */
public abstract class DependencyFilter extends AbstractArtifactsFilter {

	//依赖集合
	private final List<? extends FilterableDependency> filters;

	/**
	 * Create a new instance with the list of {@link FilterableDependency} instance(s) to
	 * use.
	 * 初始化依赖过滤器
	 * @param dependencies the source dependencies
	 */
	public DependencyFilter(List<? extends FilterableDependency> dependencies) {
		this.filters = dependencies;
	}

	/**
	 * 匹配artifact并放入集合
	 * @param artifacts
	 * @return
	 * @throws ArtifactFilterException
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Set filter(Set artifacts) throws ArtifactFilterException {
		Set result = new HashSet();
		for (Object artifact : artifacts) {
			//调用子类的过滤方法,如果返回false
			if (!filter((Artifact) artifact)) {
				//将包含的依赖放入set集合中
				result.add(artifact);
			}
		}
		return result;
	}

	//待子类实现
	protected abstract boolean filter(Artifact artifact);

	/**
	 * Check if the specified {@link org.apache.maven.artifact.Artifact} matches the
	 * specified {@link org.springframework.boot.maven.FilterableDependency}. Returns
	 * 判断artifact是否相等
	 * {@code true} if it should be excluded
	 * @param artifact the Maven {@link Artifact}
	 * @param dependency the {@link FilterableDependency}
	 * @return {@code true} if the artifact matches the dependency
	 */
	protected final boolean equals(Artifact artifact, FilterableDependency dependency) {
		//判断groupId是否相等
		if (!dependency.getGroupId().equals(artifact.getGroupId())) {
			return false;
		}
		//判断artifactId是否相等
		if (!dependency.getArtifactId().equals(artifact.getArtifactId())) {
			return false;
		}
		//判断classifier是否相等
		return (dependency.getClassifier() == null
				|| artifact.getClassifier() != null && dependency.getClassifier().equals(artifact.getClassifier()));
	}

	/**
	 * 获取依赖集合
	 * @return
	 */
	protected final List<? extends FilterableDependency> getFilters() {
		return this.filters;
	}

}
