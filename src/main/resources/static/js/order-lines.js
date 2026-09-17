/*
 * Add/remove line-item rows on the order form.
 *
 * Plain DOM, no framework: the only requirement is that the `name` attributes stay
 * as a contiguous items[0..n] sequence, because that is what Spring's data binder
 * uses to rebuild the collection on submit.
 */
(function () {
    'use strict';

    var table = document.getElementById('lines');
    var template = document.getElementById('line-template');
    var addButton = document.getElementById('add-line');
    var emptyNotice = document.getElementById('no-lines');

    if (!table || !template || !addButton) {
        return;
    }

    var body = table.querySelector('tbody');

    function rows() {
        return body.querySelectorAll('tr.line-row');
    }

    /**
     * Renumbers every row's field names after an insert or removal. Without this a
     * removal would leave a gap (items[0], items[2]) and bind a null element.
     */
    function reindex() {
        rows().forEach(function (row, index) {
            row.querySelectorAll('[name]').forEach(function (field) {
                field.name = field.name.replace(/items\[\d+\]/, 'items[' + index + ']');
            });
        });
        updateEmptyNotice();
    }

    function updateEmptyNotice() {
        if (!emptyNotice) {
            return;
        }
        var isEmpty = rows().length === 0;
        emptyNotice.style.display = isEmpty ? '' : 'none';
        table.style.display = isEmpty ? 'none' : '';
    }

    addButton.addEventListener('click', function () {
        var markup = template.innerHTML.replace(/__INDEX__/g, String(rows().length));
        body.insertAdjacentHTML('beforeend', markup);
        reindex();
    });

    // Delegated so it also covers rows added after page load. `closest()` rather than
    // checking `event.target` directly: the button now wraps an icon, so a click can
    // land on that child element rather than the button itself.
    body.addEventListener('click', function (event) {
        var button = event.target.closest('.js-remove-line');
        if (!button) {
            return;
        }
        var row = button.closest('tr.line-row');
        if (row) {
            row.remove();
            reindex();
        }
    });

    updateEmptyNotice();
})();
