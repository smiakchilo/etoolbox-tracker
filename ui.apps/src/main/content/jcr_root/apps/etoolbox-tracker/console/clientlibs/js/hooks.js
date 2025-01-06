(function(window, $, ns) {
    'use strict';

    $(document).on('click', '#hooks', onHooksButtonClick);
    const $foundationUi = $(window).adaptTo('foundation-ui');

    ns.updateHooksStatus = function (status) {
        if (status === 'on') {
            $('#hooks').attr('data-active', true).attr('title', 'Extra Hooks (active)').get(0).icon = 'gearsDelete';
        } else {
            $('#hooks').removeAttr('data-active').attr('title', 'Extra Hooks').get(0).icon = 'gearsAdd';
        }
    }

    function onHooksButtonClick(e) {
        const $button = $(e.target).closest('[is="coral-anchorbutton"]');
        const isActive = $button.is('[data-active]');
        if (isActive) {
            $foundationUi.prompt(
                'Unregister hooks',
                'Unregister extra hooks?',
                "warning",
                [
                    { text: 'Cancel' },
                    { text: 'OK', primary: true, handler: () => changeHooks('off') }
                ]);
        } else {
            $foundationUi.prompt(
                'Register hooks',
                'Register extra hooks? Only use this option in a non-production environment.',
                "warning",
                [
                    { text: 'Cancel' },
                    { text: 'OK', warning: true, handler: () => changeHooks('on') }
                ]);

        }
    }

    function changeHooks(status) {
        $foundationUi.wait();
        $.post(ns.HOOKS_ENDPOINT + '/' + status)
            .done(function (data) {
                ns.updateHooksStatus(JSON.parse(data).status);
                ns.reload();
            })
            .fail(function (response, status, error) {
                $foundationUi.alert(
                    status === 'on' ? 'Register hooks' : 'Unregister hooks',
                    'Failed to update hooks: ' + error,
                    'error');
                $foundationUi.clearWait();
            });
    }

})(window, Granite.$, window.tracker = window.tracker || {});