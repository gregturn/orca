/*
 * Copyright 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.clouddriver.pipeline.aws

import com.netflix.spinnaker.orca.clouddriver.tasks.MonitorKatoTask
import com.netflix.spinnaker.orca.clouddriver.tasks.ServerGroupCacheForceRefreshTask
import com.netflix.spinnaker.orca.clouddriver.pipeline.support.TargetServerGroupLinearStageSupport
import com.netflix.spinnaker.orca.clouddriver.tasks.aws.scalingprocess.ResumeAwsScalingProcessTask
import com.netflix.spinnaker.orca.clouddriver.tasks.aws.scalingprocess.SuspendAwsScalingProcessTask
import com.netflix.spinnaker.orca.pipeline.model.Stage
import groovy.transform.CompileStatic
import org.springframework.batch.core.Step
import org.springframework.stereotype.Component

@Component
@CompileStatic
class ModifyAwsScalingProcessStage extends TargetServerGroupLinearStageSupport {

  static final String PIPELINE_CONFIG_TYPE = "modifyAwsScalingProcess"

  ModifyAwsScalingProcessStage() {
    super(PIPELINE_CONFIG_TYPE)
    name = "Modify Scaling Process"
  }

  @Override
  public List<Step> buildSteps(Stage stage) {
    composeTargets(stage)

    def data = stage.mapTo(StageData)
    switch (data.action) {
      case StageAction.suspend:
        return [
          buildStep(stage, "suspend", SuspendAwsScalingProcessTask),
          buildStep(stage, "monitor", MonitorKatoTask),
          buildStep(stage, "forceCacheRefresh", ServerGroupCacheForceRefreshTask)
        ]
      case StageAction.resume:
        return [
          buildStep(stage, "resume", ResumeAwsScalingProcessTask),
          buildStep(stage, "monitor", MonitorKatoTask),
          buildStep(stage, "forceCacheRefresh", ServerGroupCacheForceRefreshTask)
        ]
    }
    throw new RuntimeException("No action specified!")
  }

  enum StageAction {
    suspend, resume
  }

  static class StageData {
    StageAction action
  }
}
