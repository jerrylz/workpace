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

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;

/**
 * Include过滤器
 * An {@link ArtifactsFilter} that filters out any artifact not matching an
 * {@link Include}.
 *
 * @author David Turanski
 * @since 1.2.0
 */
public class IncludeFilter extends DependencyFilter {

	public IncludeFilter(List<Include> includes) {
		super(includes);
	}

	/**
	 * 过滤符合条件的依赖
	 * @param artifact
	 * @return
	 */
	@Override
	protected boolean filter(Artifact artifact) {
		for (FilterableDependency dependency : getFilters()) {
			//如果匹配返回false
			if (equals(artifact, dependency)) {
				return false;
			}
		}
		//未匹配则返回true
		return true;
	}

}
