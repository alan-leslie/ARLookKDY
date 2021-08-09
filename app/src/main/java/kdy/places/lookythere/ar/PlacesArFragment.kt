// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package kdy.places.lookythere.ar

import android.Manifest
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment

class PlacesArFragment : ArFragment() {
    override fun getSessionConfiguration(session: Session?): Config {
        val config = Config(session)
//        DISABLED(0),
//        HORIZONTAL(1),

        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
//        config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
//        session?.configure(config)

        return config
    }

    override fun getAdditionalPermissions(): Array<String> =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION).toTypedArray()
}
