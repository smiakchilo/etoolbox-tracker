(function(window, $, ns) {
    'use strict';

    const DIALOG_ID = 'etoolbox-tracker-settings';

    ns.openSettingsDialog = function () {
        const $dialogTemplate = $($('#settings-dialog').prop('content'));
        const $dialogFooterTemplate = $($('#dialog-ok-cancel').prop('content'));
        const $dialog = provideSettingsDialog(
            {
                id: DIALOG_ID,
                header: { innerHTML: 'EToolbox Tracker Settings' },
                content: { innerHTML: $('<div>').append($dialogTemplate).html() },
                footer: { innerHTML: $('<div>').append($dialogFooterTemplate).html() },
                movable: true,
                backdrop: 'static'
            });
        requestAnimationFrame(() => {
            populateSettings($dialog);
            $dialog[0].show();
        });
    };

    function provideSettingsDialog(content) {
        let $dialog = $('#' + content.id);
        if ($dialog.length) {
            return $dialog;
        }

        const dialog = new Coral.Dialog().set(content);
        document.body.appendChild(dialog);

        $dialog = $(dialog);
        $dialog.find('button[variant="primary"]').on('click', () => onAcceptButtonClick($dialog));
        $dialog.find('[data-visible-when]')
            .each((_, field) => {
                const $field = $(field);
                const triggerName = $field.data('visible-when');
                $dialog
                    .find(`[name="${triggerName}"]`)
                    .on('change', (e) => onVisibilityTrigger(e, $field));
            });
        return $dialog;
    }

    /* --------------
       Event handlers
       -------------- */

    function onAcceptButtonClick($dialog) {
        storeSettings($dialog);
        $dialog[0].hide();

    }

    function onVisibilityTrigger(e, $field) {
        const isOn = extractValue($(e.target));
        $field.attr('hidden', !isOn);
    }

    /* --------------
       Settings logic
       -------------- */

    function populateSettings($dialog) {
        ns.settings.keys().forEach((key) => {
            $dialog.find(`input[name="${key}"],textarea[name="${key}"]`).each((_, field) => populateSetting($(field), ns.settings.get(key)));
        });
    }

    function populateSetting($field, value) {
        if ($field.is('[type="checkbox"]')) {
            $field.parent().attr('checked', value);
        } else {
            $field.val(value);
        }
        $field.trigger('change');
    }

    function storeSettings($dialog) {
        const valueMap = {};
        $dialog.find('input[name],textarea[name]').each((_, field) => {
            const $field = $(field);
            valueMap[$field.attr('name')] = extractValue($field);
        });
        ns.settings.set(valueMap);
    }

    function extractValue($field) {
        if ($field.is('coral-checkbox')) {
            return !!$field.attr('checked');
        }
        if ($field.is('[type="checkbox"]')) {
            return !!$field.parent().attr('checked');
        }
        return $field.val();
    }

})(window, Granite.$, window.tracker = window.tracker || {});