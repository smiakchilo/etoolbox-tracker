(function (window, $, ns) {
    'use strict';


    /* -------
       Globals
       ------- */

    ns.TRACK_ENDPOINT = '/apps/etoolbox-tracker/track.json';
    ns.HOOKS_ENDPOINT = '/apps/etoolbox-tracker/hooks';

    /* ------
       Events
       ------ */

    window.addEventListener('load', onLoad);
    const $document = $(document);
    $document.on('click', '#refresh', onRefreshButtonClick);
    $document.on('click', '#settings', onSettingsButtonClick);
    $document.on('keydown', '[name="url"]', $.debounce(250, onUrlChanged));

    /* --------------
       Event handlers
       -------------- */

    async function onLoad() {
        const url = window.location.href.split('console.html')[1];
        $('#refresh').toggleClass('disabled', !url);
        if (url) {
            $('[name="url"]').val(url);
            ns.reload();
        }
    }

    function onRefreshButtonClick() {
        ns.reload();
    }

    function onSettingsButtonClick() {
        ns.openSettingsDialog();
    }

    function onUrlChanged(e) {
        const url = $('[name="url"]').val();
        $('#refresh').toggleClass('disabled', !url);
        urlToWindowLocation(url);
        if (url && e.which === 13) {
            onRefreshButtonClick();
        }
    }

    function urlToWindowLocation(url) {
        const cleanLocation = window.location.href.split('console.html')[0] + 'console.html';
        if (!url) {
            window.history.pushState({}, null, cleanLocation)
            return;
        }
        if (!url.startsWith('/')) {
            url = '/' + url;
        }
        window.history.pushState({}, null, cleanLocation + url);
    }


})(window, Granite.$, window.tracker = window.tracker || {});
