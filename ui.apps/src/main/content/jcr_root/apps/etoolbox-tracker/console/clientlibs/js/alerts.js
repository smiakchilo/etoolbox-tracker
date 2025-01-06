(function(window, $, ns) {
    'use strict';

    ns.alert = function (message) {
        const $alertTemplate = $($('#alert').prop('content'));
        const $alert = $alertTemplate.clone();
        $alert.find('.text').html(message);
        $alert.appendTo($('.alerts'));
    };

    ns.clearAlerts = function () {
        $('.alerts').empty();
    };

})(window, Granite.$, window.tracker = window.tracker || {});