const registeredTools = [
  {
    type: "function",
    function: {
      name: "open_app",
      description: "Open an application on the Android device by package name or common app name (e.g. YouTube, Maps, WhatsApp, Settings)",
      parameters: {
        type: "object",
        properties: {
          package: {
            type: "string",
            description: "App package name or simple name like 'com.google.android.youtube' or 'youtube'"
          }
        },
        required: ["package"]
      }
    }
  },
  {
    type: "function",
    function: {
      name: "open_url",
      description: "Open a URL or perform a web search in browser",
      parameters: {
        type: "object",
        properties: {
          url: {
            type: "string",
            description: "Full URL starting with http:// or https://"
          }
        },
        required: ["url"]
      }
    }
  },
  {
    type: "function",
    function: {
      name: "search_web",
      description: "Perform a Google web search",
      parameters: {
        type: "object",
        properties: {
          query: {
            type: "string",
            description: "Search query"
          }
        },
        required: ["query"]
      }
    }
  },
  {
    type: "function",
    function: {
      name: "set_volume",
      description: "Set the device media volume level percentage (0 to 100)",
      parameters: {
        type: "object",
        properties: {
          level: {
            type: "number",
            description: "Target volume level percentage from 0 to 100"
          }
        },
        required: ["level"]
      }
    }
  },
  {
    type: "function",
    function: {
      name: "increase_volume",
      description: "Increase media volume by a step",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "decrease_volume",
      description: "Decrease media volume by a step",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "play_music",
      description: "Start or resume music playback",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "pause_music",
      description: "Pause current music playback",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "next_track",
      description: "Skip to next music track",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "previous_track",
      description: "Go back to previous music track",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "get_battery",
      description: "Get current battery level and charging state of the device",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "get_time",
      description: "Get current local time",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "set_alarm",
      description: "Set an alarm for a specific hour and minute",
      parameters: {
        type: "object",
        properties: {
          hour: { type: "number", description: "Hour (0-23)" },
          minute: { type: "number", description: "Minute (0-59)" },
          label: { type: "string", description: "Optional alarm title" }
        },
        required: ["hour", "minute"]
      }
    }
  },
  {
    type: "function",
    function: {
      name: "take_screenshot",
      description: "Trigger device screenshot using accessibility service",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "open_settings",
      description: "Open Android system settings",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "go_home",
      description: "Navigate to home screen using system accessibility action",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "go_back",
      description: "Trigger back button navigation using system accessibility action",
      parameters: { type: "object", properties: {} }
    }
  }
];

module.exports = {
  registeredTools
};
