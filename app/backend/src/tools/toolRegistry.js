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
  },
  {
    type: "function",
    function: {
      name: "open_recent_apps",
      description: "Open recent apps overview using accessibility action",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "scroll_down",
      description: "Scroll down on the active screen",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "scroll_up",
      description: "Scroll up on the active screen",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "click_known_element",
      description: "Click a UI element matching visible text or view ID",
      parameters: {
        type: "object",
        properties: {
          text: { type: "string", description: "Text or ID of element to click" }
        },
        required: ["text"]
      }
    }
  },
  {
    type: "function",
    function: {
      name: "type_text_into_supported_field",
      description: "Type text into an active input field",
      parameters: {
        type: "object",
        properties: {
          field: { type: "string", description: "Field label or ID" },
          text: { type: "string", description: "Text to type" }
        },
        required: ["field", "text"]
      }
    }
  },
  {
    type: "function",
    function: {
      name: "read_visible_screen",
      description: "Extract and summarize visible text elements on screen using accessibility API",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "read_notifications",
      description: "Summarize current active device notifications locally",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "open_camera",
      description: "Open the system camera app",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "open_gallery",
      description: "Open system photo gallery",
      parameters: { type: "object", properties: {} }
    }
  },
  {
    type: "function",
    function: {
      name: "make_call",
      description: "Initiate phone call to contact or number",
      parameters: {
        type: "object",
        properties: {
          phone_number: { type: "string", description: "Phone number or contact name" }
        },
        required: ["phone_number"]
      }
    }
  },
  {
    type: "function",
    function: {
      name: "send_message",
      description: "Draft or send an SMS message",
      parameters: {
        type: "object",
        properties: {
          phone_number: { type: "string", description: "Phone number or contact name" },
          message: { type: "string", description: "Message body" }
        },
        required: ["phone_number", "message"]
      }
    }
  }
];

module.exports = {
  registeredTools
};
