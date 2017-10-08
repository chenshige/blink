var __blink_browser_object__ = function() {
    var objects = document.getElementsByTagName('video');
    if (objects === "undefined" || objects === null
        || objects.length > 1) {
        return null;
    }

    var object = objects[0];
    if (object === "undefined" || object === null) {
        return null;
    }
    return object;
};

var __blink_browser__ = {
    getMediaDuration : function() {
        if (__blink_browser_object__() === null) {
            return -1;
        }
        var duration = __blink_browser_object__().duration;
        if (!isNaN(duration)) {
            __blink__.getMediaDuration(duration);
        }
    },

    seek : function(time) {
        if (__blink_browser_object__() === null) {
            return -1;
        }
        __blink_browser_object__().currentTime += time;
    },

    seekTo : function(time) {
        if (__blink_browser_object__() === null) {
            return -1;
        }
        __blink_browser_object__().currentTime = time;
    },

    getCurrentPlayTime : function() {
        if (__blink_browser_object__() === null) {
            return -1;
        }
        var currentTime = __blink_browser_object__().currentTime;
        if (!isNaN(currentTime)) {
            __blink__.getCurrentPlayTime(currentTime);
        }
    },

    retrieveVideo : function() {
        if (__blink_browser_object__() === null) {
            __blink__.canGetVideoElement(false);
        } else {
            __blink__.canGetVideoElement(true);
        }
    },

    exitFullScreen : function() {
        document.webkitCancelFullScreen();
    },

    pause : function() {
        if (__blink_browser_object__() === null) {
            return -1;
        }
        __blink_browser_object__().pause();
    },

    isPaused : function() {
        if (__blink_browser_object__() === null) {
            return -1;
        }
        var paused = __blink_browser_object__().paused;
        if (paused != "undefined" && paused != null) {
            __blink__.isPaused(paused);
        }
    },

    play : function() {
        if (__blink_browser_object__() === null) {
            return -1;
        }
        __blink_browser_object__().play();
    },

    playEvent : function() {
        __blink__.isPaused(false);
        __blink__.played();
    },

    pauseEvent : function() {
        __blink__.isPaused(true);
        __blink__.paused();
    },

    timeupdateEvent : function() {
        var currentTime = __blink_browser_object__().currentTime;
        if (!isNaN(currentTime)) {
            __blink__.getCurrentPlayTime(currentTime);
        }
    },

    registerListener : function() {
        if (__blink_browser_object__() === null) {
            return -1;
        }
        __blink_browser_object__().addEventListener('play', this.playEvent);
        __blink_browser_object__().addEventListener('pause', this.pauseEvent);
        __blink_browser_object__().addEventListener('timeupdate', this.timeupdateEvent);
    },

    unregisterListener : function() {
        if (__blink_browser_object__() === null) {
            return -1;
        }
        __blink_browser_object__().removeEventListener('play', this.playEvent);
        __blink_browser_object__().removeEventListener('pause', this.pauseEvent);
        __blink_browser_object__().removeEventListener('timeupdate', this.timeupdateEvent);
    },
};

var __blink_media_hack__ = {
        getVideoShadowStyle : function() {
            if (__blink_browser_object__() === null) {
                return -1;
            }
            var style = window.getComputedStyle(__blink_browser_object__(), '-webkit-media-controls');
            return style;
        },

        replaceVideoShadowStyle : function() {
            if (__blink_browser_object__().hasAttribute("controls")) {
                __blink_browser_object__().removeAttribute("controls");
                webview_version_low = true;
            }

            var tagHead = document.documentElement.firstChild;
            tagStyle = document.createElement("style");
            tagStyle.setAttribute("type", "text/css");
            tagStyle.appendChild(document.createTextNode("video::-webkit-media-controls {display:none !important;}"));

            tagHead.appendChild(tagStyle);
        },

        resetVideoShadowStyle : function() {
            if (__blink_browser_object__() === null) {
                return -1;
            }

            var tagHead = document.documentElement.firstChild;
            var child = tagHead.firstChild;
            while (child) {
                if (child === tagStyle) {
                    tagHead.removeChild(tagStyle);
                    break;
                }
                child = child.nextSibling;
            }

            if (webview_version_low) {
                __blink_browser_object__().setAttribute("controls","");
            }
        },
}
