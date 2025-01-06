const TASK_BATCH_SIZE = 10;
const NON_EXPANDED_CATEGORIES = ['code', 'model'];

(function(window, $, ns) {
    'use strict';

    const treeRenderingTasks = [];
    let storedData;

    ns.reload = reload;

    /* ------
       Events
       ------ */

    const $document = $(document);
    window.addEventListener('tracker-settings-changed', onSettingsChanged);
    $document.on('coral-tree:change', 'coral-tree', onTreeSelectionChanged);
    $document.on('click', '#statistics', onStatisticsButtonClick);
    $document.on('dblclick', 'coral-tree-item-content', onTreeItemDblClick);

    function onSettingsChanged(e) {
        if (!storedData) {
            return reload();
        }
        renderTree(storedData, new ActiveSettings(e.detail.settings));
    }

    function onTreeSelectionChanged() {
        $('#statistics').toggleClass('disabled', !$('coral-tree-item[selected]:visible').length);
    }

    function onStatisticsButtonClick() {
        displayStatistics();
    }

    function onTreeItemDblClick() {
        const $this = $(this);
        const $treeItem = $this.closest('coral-tree-item');
        if (!$treeItem.length || $treeItem.is('.coral3-Tree-item--leaf')) {
            return false;
        }
        $treeItem[0].expanded = !$treeItem[0].expanded;
        return false;
    }

    /* ------------------
       Grid manipulations
       ------------------ */

    function reload() {
        const url = $('[name="url"]').val();
        if (!url) {
            return;
        }
        const foundationUi = $(window).adaptTo('foundation-ui');
        foundationUi.wait();
        loadData(url).then((data) => {
            storedData = data;
            renderAlerts(data);
            if (data.hooks) {
                ns.updateHooksStatus(data.hooks);
            }
            renderTree(data, new ActiveSettings(ns.settings.asMap()));
            foundationUi.clearWait();
        });
    }

    async function loadData(url) {
        try {
            const response = await fetch(ns.TRACK_ENDPOINT + url);
            if (!response.ok) {
                return {status: response.status};
            }
            return await response.json();
        } catch (e) {
            console.error(e);
            return { error: e.message || e};
        }
    }

    /* -----------------
       Content rendering
       ----------------- */

    function renderAlerts(data) {
        ns.clearAlerts();
        if (data.error) {
            ns.alert(data.error, 'error');
        } else if (data.status && data.status !== 200) {
            ns.alert('HTTP Status: ' + data.status, 'error');
        }
    }

    function renderTree(data, settings) {
        const $tree = $('.grid coral-tree');
        treeRenderingTasks.length = 0;
        treeRenderingTasks.push(() => {
            $tree.empty();
            onTreeSelectionChanged();
        });
        if (data.records) {
            data.records.forEach((record) => {
                const newTask = () => addTreeNode($tree[0], record, settings);
                treeRenderingTasks.push(newTask);
            });
        }
        requestAnimationFrame(renderTreeAsync);
    }

    function renderTreeAsync() {
        for (let i = 0; i < TASK_BATCH_SIZE; i++) {
            const nextTask = treeRenderingTasks.shift();
            if (!nextTask) {
                return;
            }
            nextTask();
        }
        if (treeRenderingTasks.length) {
            requestAnimationFrame(renderTreeAsync);
        }
    }

    function addTreeNode(parent, record, settings) {
        const innerHtml = `
            <span class="node-icon icon--${record.category || 'default'}"></span>
            <span class="name" title="${getTooltipContent(record)}">${record.label}</span>
            <div class="time">${formatTime(record.time)}</div>
        `;
        const newItem = parent.items.add({
            content: {innerHTML: innerHtml},
            expanded: shouldExpand(record, settings),
            variant: 'leaf',
        });
        parent.variant = 'drilldown';
        newItem.classList.toggle('bookmark', settings.isBookmarked(record));
        newItem.classList.toggle('slow', settings.isSlow(record));
        newItem.classList.toggle('zero-time', record.time === 0);
        const hasChildren = record.records && record.records.length;
        if (hasChildren) {
            const filteredRecords = record.records.filter((record) => settings.isRenderable(record));
            newItem.classList.add('has-children');
            newItem.classList.toggle('is-filtered', filteredRecords.length < record.records.length);
            filteredRecords
                .forEach((record) => {
                    const newTask = () => addTreeNode(newItem, record, settings);
                    treeRenderingTasks.push(newTask);
            });
        }
    }

    function shouldExpand(record, settings) {
        if (!record.records || !record.records.length || !NON_EXPANDED_CATEGORIES.includes(record.category)) {
            return true;
        }
        return record.time >= settings.slowThreshold || record.records.some((r) => settings.isSlow(r));
    }

    /* ------------------
       Settings/Filtering
       ------------------ */

    class ActiveSettings {
        constructor(rawSettings) {
            this.hideThreshold = rawSettings.hideFasterThan ? Number.parseFloat(rawSettings.hideThreshold) : 0;
            this.slowThreshold = rawSettings.markSlowerThan ? Number.parseFloat(rawSettings.slowThreshold) : Number.MAX_VALUE;
            this.bookmarkPatterns = rawSettings.bookmarks
                ? rawSettings.bookmarks
                    .split('\n')
                    .map((p) => p.trim())
                    .filter((p) => p)
                    .map((p) => new RegExp(p))
                : [];
        }

        isRenderable(record) {
            return record.time >= this.hideThreshold;
        }

        isSlow(record) {
            return record.ownTime >= this.slowThreshold
                || (!isComplex(record) && record.time >= this.slowThreshold);
        }

        isBookmarked(record) {
            return this.bookmarkPatterns.some((p) => p.test(record.label));
        }
    }

    /* ----------
       Statistics
       ---------- */

    function displayStatistics() {
        const $selectedItem = $('coral-tree-item[selected]:visible');
        const $foundationUi = $(window).adaptTo('foundation-ui');
        if (!$selectedItem.length) {
            $foundationUi.alert('Statistics', 'Selection is empty', 'error');
            return;
        }
        const label = ($selectedItem.find('.name:first').text() || '').replace(/#\d+$/, '');
        const statistics = { time: 0.0, ownTime: 0.0, count: 0 };
        collectTimingStatistics(storedData, label, statistics);
        const aggrTime = statistics.time.toFixed(1);
        const aggrOwnTime = statistics.ownTime.toFixed(1);
        let text = `
              <p class="statistics">Number of entries: ${statistics.count}</p>
              <p class="statistics time">Aggregate time: ${formatTime(aggrTime)}</p>`;
        if (aggrTime !== aggrOwnTime) {
            text += `<p class="statistics time">Aggregate own time (minus children): ${formatTime(aggrOwnTime)}</p>`;
        }
        $foundationUi.alert('Statistics for ' + label, text, 'info');
    }

    function collectTimingStatistics(current, match, accumulator) {
        const currentLabel = (current.label || '').replace(/#\d+$/, '');
        if (currentLabel === match) {
            accumulator.count += 1;
            if ('time' in current) {
                accumulator.time += current.time;
            }
            if ('ownTime' in current) {
                accumulator.ownTime += current.ownTime;
            }
        }
        if (!current.records || !current.records.length) {
            return;
        }
        for (const record of current.records) {
            collectTimingStatistics(record, match, accumulator);
        }
    }

    /* -----------------
       Records utilities
       ----------------- */

    function isComplex(record) {
        return record.records && record.records.some((r) => isComponentOrHasComponent(r));
    }

    function isComponentOrHasComponent(record) {
        if (record.category === 'component') {
            return true;
        }
        return record.records && record.records.some((r) => isComponentOrHasComponent(r));
    }

    /* --------------
       Misc utilities
       -------------- */

    function getTooltipContent(record) {
        let category = record.category || '';
        if (category === 'postconstruct') {
            category = '';
        }
        if (category.length) {
            category = category.replace(/-/g, ' ');
            category = capitalize(category) + ': ';
        }

        const timeString = record.time === record.ownTime
            ? `Time: ${record.time} ms`
            : `Total time: ${record.time} ms\nOwn time (minus children): ${record.ownTime} ms`;

        let comment = record.comment || '';
        if (/^[a-z]/.test(comment)) {
            comment = capitalize(comment);
        }
        if (comment) {
            comment = '\n' + comment;
        }
        return `${category}${record.label}\n${timeString}${comment}`;
    }

    function capitalize(value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    function formatTime(value) {
        const timeStr = String(value);
        let mainPart = timeStr;
        let fractionPart = '';
        if (timeStr.includes('.')) {
            mainPart = timeStr.split('.')[0];
            fractionPart = '.' + timeStr.split('.')[1];
        } else if (timeStr === '0') {
            mainPart = '&ndash;';
        }
        return `${mainPart}<span class="fract">${fractionPart}</span>`;
    }

})(window, Granite.$, window.tracker = window.tracker || {});