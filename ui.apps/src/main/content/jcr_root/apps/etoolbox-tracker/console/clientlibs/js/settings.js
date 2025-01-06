(function(window, ns) {
    'use strict';

    const SETTINGS_KEY = 'etoolbox-tracker';
    const DEFAULTS = {
        hideFasterThan: true,
        hideThreshold: '0.1',
        markSlowerThan: true,
        slowThreshold: '30'
    };

    ns.settings = {
        asMap: function () {
            const settingsText = window.localStorage.getItem(SETTINGS_KEY);
            if (!settingsText) {
                return DEFAULTS;
            }
            try {
                return JSON.parse(settingsText);
            } catch (e) {
                console.error('Failed to parse settings', e);
                return DEFAULTS;
            }
        },
        get: function (key) {
            return this.asMap()[key];
        },
        keys: function () {
            return Object.keys(this.asMap());
        },
        set: function (key, value) {
            if (typeof key === 'object' && !value) {
                window.localStorage.setItem(SETTINGS_KEY, JSON.stringify(key));
                window.dispatchEvent(new CustomEvent('tracker-settings-changed', { detail: { settings: this.asMap() } }));
                return;
            }
            const settings = this.asMap();
            settings[key] = value;
            window.localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
            window.dispatchEvent(new CustomEvent('tracker-settings-changed', { detail: { settings: this.asMap() } }));
        }
    }

})(window, window.tracker = window.tracker || {});