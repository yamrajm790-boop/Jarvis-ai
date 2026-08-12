/**
 * Registry of valid Android Tools that JARVIS AI can invoke.
 */

const toolRegistry = {
  open_app: {
    description: "Open an application installed on the Android device by name or package",
    parameters: {
      type: "object",
      properties: {
        package: { type: "string", description: "Android package name (e.g. com.google.android.youtube) or app name (e.g. YouTube)" }
      },
      required: ["package"]
    }
  },
  open_url: {
    description: "Open a web URL in the browser",
    parameters: {
      type: "object",
      properties: {
        url: { type: "string", description: "Full URL to open (e.g. https://google.com)" }
      },
      required: ["url"]
    }
  },
  search_web: {
    description: "Perform a Google / Web Search on the Android device",
    parameters: {
      type: "object",
      properties: {
        query: { type: "string", description: "Search query string" }
      },
      required: ["query"]
    }
  },
  set_volume: {
    description: "Set device media volume percentage (0 - 100)",
    parameters: {
      type: "object",
      properties: {
        level: { type: "integer", description: "Volume level from 0 to 100" }
      },
      required: ["level"]
    }
  },
  increase_volume: {
    description: "Increase media volume by a percentage or step",
    parameters: {
      type: "object",
      properties: {
        step: { type: "integer", description: "Percentage step to increase" }
      }
    }
  },
  decrease_volume: {
    description: "Decrease media volume by a percentage or step",
    parameters: {
      type: "object",
      properties: {
        step: { type: "integer", description: "Percentage step to decrease" }
      }
    }
  },
  play_music: {
    description: "Start or resume media audio playback",
    parameters: { type: "object", properties: {} }
  },
  pause_music: {
    description: "Pause media audio playback",
    parameters: { type: "object", properties: {} }
  },
  resume_music: {
    description: "Resume media audio playback",
    parameters: { type: "object", properties: {} }
  },
  next_track: {
    description: "Skip to the next media track",
    parameters: { type: "object", properties: {} }
  },
  previous_track: {
    description: "Skip to the previous media track",
    parameters: { type: "object", properties: {} }
  },
  get_time: {
    description: "Get the current time on the device",
    parameters: { type: "object", properties: {} }
  },
  get_date: {
    description: "Get current date on the device",
    parameters: { type: "object", properties: {} }
  },
  set_alarm: {
    description: "Set an alarm clock for a specific hour and minute",
    parameters: {
      type: "object",
      properties: {
        hour: { type: "integer", description: "Hour of day (0-23)" },
        minute: { type: "integer", description: "Minute of hour (0-59)" },
        label: { type: "string", description: "Alarm description label" }
      },
      required: ["hour", "minute"]
    }
  },
  set_timer: {
    description: "Set a countdown timer in seconds",
    parameters: {
      type: "object",
      properties: {
        seconds: { type: "integer", description: "Timer length in seconds" },
        label: { type: "string", description: "Timer description label" }
      },
      required: ["seconds"]
    }
  },
  get_battery: {
    description: "Get current battery level and charging state",
    parameters: { type: "object", properties: {} }
  },
  get_device_info: {
    description: "Get device info like model, OS version, and network status",
    parameters: { type: "object", properties: {} }
  },
  open_settings: {
    description: "Open main Android device settings screen",
    parameters: { type: "object", properties: {} }
  },
  open_wifi_settings: {
    description: "Open Android Wi-Fi settings screen",
    parameters: { type: "object", properties: {} }
  },
  open_bluetooth_settings: {
    description: "Open Android Bluetooth settings screen",
    parameters: { type: "object", properties: {} }
  },
  make_call: {
    description: "Dial a phone number or contact",
    parameters: {
      type: "object",
      properties: {
        phone_number: { type: "string", description: "Phone number or contact name" }
      },
      required: ["phone_number"]
    }
  },
  send_message: {
    description: "Draft or send an SMS message",
    parameters: {
      type: "object",
      properties: {
        phone_number: { type: "string", description: "Recipient phone number or contact name" },
        message: { type: "string", description: "Message body text" }
      },
      required: ["phone_number", "message"]
    }
  },
  take_screenshot: {
    description: "Take a device screenshot if supported",
    parameters: { type: "object", properties: {} }
  },
  go_home: {
    description: "Navigate to Android home screen via accessibility service",
    parameters: { type: "object", properties: {} }
  },
  go_back: {
    description: "Perform system back button action via accessibility service",
    parameters: { type: "object", properties: {} }
  }
};

module.exports = toolRegistry;
