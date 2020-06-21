/**
 *
 *  Copyright 2020 Nick Davidson 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	Current Features:
 *		Auto Turn on when arrival (after sunset)
 *		Auto Turn on when motion detected (after sunset)
 *		Auto Turn off after set time
 *		Auto Aware to leave lights on when set to on manually
 *		Force Switch to always turn off after being on for set time
 *
 *	Future Plans:
 *		Fix bug where motion detection dosent reset the turn off timer
 *
 */
definition(
    name: "better-smart-lighting",
    namespace: "nd0905",
    author: "Nick Davidson",
    description: "Turns on and off switches based on proximity sensors and timers",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	section("Select Devices") {
		input "switches", "capability.switch", title: "Select Lights", multiple: true
        input "motion1", "capability.motionSensor", title: "Select Sensors", multiple: true, required: false
	}
	section("Turn it off in... ") {
		input "minutesLater", "number", title: "Minutes?"
	}
    section("When who arrives:") {
		input "people", "capability.presenceSensor", multiple: true, required: false
    }
    section("Always turn off after that long?") {
    	input "force", "BOOLEAN", title: "true = Force, false = Only on Trigger", required: true, defaultValue: false
    }
    section("Never Turn Off After Event?") {
    	input "neverTime", "BOOLEAN", title: "true = Turn Off After Time, false = Never Turn Off", required: true, defaultValue: true
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(people, "presence", arrivalHandler)
    subscribe(motion1, "motion", motionHandler)
    subscribe(switches, "switch", timedOff)
}

def timedOff(evt) {
	if( evt.value == "on" && force == "true") {
    	switches.on()
        if ( neverTime == "true" ) { switches.off([delay: 1000 * 60 * minutesLater]) }
    }
}

def arrivalHandler(evt) {
	def now = new Date()
	def sunTime = getSunriseAndSunset();
	if( evt.value == "not present" && everyoneIsAway()) {
		switches.off()
    } else if( evt.value == "present" && (now > sunTime.sunset) ) {
        for ( swich in switches ) {
        	if ( swich.currentValue("switch") == "off" ){
        		swich.on()
        		if ( neverTime == "true" ) { swich.off([delay: 1000 * 60 * minutesLater]) }
            }
        }
	}
}

def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"
	def now = new Date()
	def sunTime = getSunriseAndSunset();
	if (evt.value == "active" && (now > sunTime.sunset)) {
		log.debug "turning on lights"
		for ( swich in switches ) {
        	if ( swich.currentValue("switch") == "off" ){
        		swich.on()
                if ( neverTime == "true" ) { swich.off([delay: 1000 * 60 * minutesLater]) }
            }
        }
	} else if (evt.value == "inactive" /*&& (now > sunTime.sunset)*/) {
    	
    }
}

private everyoneIsAway() {
	def result = true
	for (person in people) {
		if (person.currentPresence == "present") {
			result = false
			break
		}
	}
	log.debug "everyoneIsAway: $result"
	return result
}