/**
 * WARNING: This file is improved.
 */

(function ($) {
    $.fn.extend({
        smartpaginator: function (options) {
            var settings = $.extend({
                totalrecords: 0,
                recordsperpage: 0,
                length: 10,
                next: 'Next',
                prev: 'Prev',
                first: 'First',
                last: 'Last',
                go: 'Go',
                of: 'of',
                theme: 'green',
                display: 'double',
                initval: 1,
                datacontainer: '', //data container id
                dataelement: '', //children elements to be filtered e.g. tr or div
                onchange: null
            }, options);
            return this.each(function () {
                var currentPage = 0;
                var startPage = 0;
                var totalpages = parseInt(settings.totalrecords / settings.recordsperpage);
                if (settings.totalrecords % settings.recordsperpage > 0) totalpages++;
                var initialized = false;
                var container = $(this).addClass('pager').addClass(settings.theme);
                container.find('ul').remove();
                container.find('div').remove();
                container.find('span').remove();
                var dataContainer;
                var dataElements;
                if (settings.datacontainer != '') {
                    dataContainer = $('#' + settings.datacontainer);
                    dataElements = $('' + settings.dataelement + '', dataContainer);
                }
                var list = $('<ul/>');
                var btnPrev = $('<div/>').append(settings.prev).click(function () { currentPage = parseInt(list.find('li a.active').text()) - 1; navigate(--currentPage); }).addClass('btn');
                var btnNext = $('<div/>').append(settings.next).click(function () { currentPage = parseInt(list.find('li a.active').text()); navigate(currentPage); }).addClass('btn');
                var btnFirst = $('<div/>').append(settings.first).click(function () { currentPage = 0; navigate(0); }).addClass('btn');
                var btnLast = $('<div/>').append(settings.last).click(function () { currentPage = totalpages - 1; navigate(currentPage); }).addClass('btn');
                var inputPage = $('<input/>').attr('type', 'text').keydown(function (e) {
                    if (isTextSelected(inputPage)) inputPage.val('');
                    if (e.which >= 48 && e.which < 58) {
                        var value = parseInt(inputPage.val() + (e.which - 48));
                        if (!(value > 0 && value <= totalpages)) e.preventDefault();
                    } else if (!(e.which == 8 || e.which == 46)) e.preventDefault();
                });
                var btnGo = $('<input/>').attr('type', 'button').attr('value', settings.go).addClass('btn').click(function () { if (inputPage.val() == '') return false; else { currentPage = parseInt(inputPage.val()) - 1; navigate(currentPage); } });
                container.append(btnFirst).append(btnPrev).append(list).append(btnNext).append(btnLast).append($('<div/>').addClass('short').append(inputPage).append(btnGo));
                if (settings.display == 'single') {
                    btnGo.css('display', 'none');
                    inputPage.css('display', 'none');
                }
                buildNavigation(startPage);
                if (settings.initval == 0) settings.initval = 1;
                currentPage = settings.initval - 1;
                navigate(currentPage);
                initialized = true;
                function showLabels(pageIndex) {
                    container.find('.pages_indication').remove();
                    var upper = (pageIndex + 1) * settings.recordsperpage;
                    if (upper > settings.totalrecords) upper = settings.totalrecords;
                    container.append($('<div/>').addClass('pages_indication')
                                             .append($('<span/>').append($('<b/>').text(pageIndex * settings.recordsperpage + 1)))
                                             .append($('<span/>').text('-'))
                                             .append($('<span/>').append($('<b/>').text(upper)))
                                             .append($('<span/>').text(settings.of))
                                             .append($('<span/>').append($('<b/>').text(settings.totalrecords))));
                }
                function buildNavigation(aStartPage) {
                    list.find('li').remove();
                    if (settings.totalrecords <= settings.recordsperpage) return;
                    var clickFunc = function () {
                        currentPage = aStartPage + $(this).closest('li').prevAll().length;
                        navigate(currentPage);
                    };
                    for (var i = aStartPage; i < aStartPage + settings.length; i++) {
                        if (i == totalpages) break;
                        list.append($('<li/>')
                                    .append($('<a>').attr('id', (i + 1)).addClass(settings.theme).addClass('normal')
                                    .attr('href', 'javascript:void(0)')
                                    .text(i + 1))
                                    .click(clickFunc));
                    }
                    showLabels(aStartPage);
                    inputPage.val((aStartPage + 1));
                    list.find('li a').addClass(settings.theme).removeClass('active');
                    list.find('li:eq(0) a').addClass(settings.theme).addClass('active');
                    //set width of paginator
                    /*var sW = list.find('li:eq(0)').outerWidth(true) + parseInt(list.find('li:eq(0)').css('margin-left'));
                    var width = sW * list.find('li').length;
                    list.css({ width: width });*/
                    showRequiredButtons();
                }
                function navigate(topage) {
                    //make sure the page in between min and max page count
                    var index = topage;
                    var mid = settings.length / 2;
                    if (settings.length % 2 > 0) mid = (settings.length + 1) / 2;
                    var startIndex = 0;
                    if (topage >= 0 && topage < totalpages) {
                        if (topage >= mid) {
                            if (totalpages - topage > mid)
                                startIndex = topage - (mid - 1);
                            else if (totalpages > settings.length)
                                startIndex = totalpages - settings.length;
                        }
                        buildNavigation(startIndex); showLabels(currentPage);
                        list.find('li a').removeClass('active');
                        inputPage.val(currentPage + 1);
                        list.find('li a[id="' + (index + 1) + '"]').addClass('active');
                        var recordStartIndex = currentPage * settings.recordsperpage;
                        var recordsEndIndex = recordStartIndex + settings.recordsperpage;
                        if (recordsEndIndex > settings.totalrecords)
                            recordsEndIndex = settings.totalrecords % recordsEndIndex;
                        if (initialized) {
                            if (settings.onchange != null) {
                                settings.onchange((currentPage + 1), recordStartIndex, recordsEndIndex);
                            }
                        }
                        if (dataContainer != null) {
                            if (dataContainer.length > 0) {
                                //hide all elements first
                                dataElements.css('display', 'none');
                                //display elements that need to be displayed
                                if ($(dataElements[0]).find('th').length > 0) { //if there is a header, keep it visible always
                                    $(dataElements[0]).css('display', '');
                                    recordStartIndex++;
                                    recordsEndIndex++;
                                }
                                for (var i = recordStartIndex; i < recordsEndIndex; i++)
                                    $(dataElements[i]).css('display', '');
                            }
                        }

                        showRequiredButtons();
                    }
                }
                function showRequiredButtons() {
                    if (totalpages > settings.length) {
                        if (currentPage > 0) { btnPrev.css('display', ''); }
                        else { btnPrev.css('display', 'none'); }
                        const offset = (settings.length % 2) !== 0 ? 0 : 1;
                        if (currentPage > settings.length / 2 - offset) { btnFirst.css('display', ''); }
                        else { btnFirst.css('display', 'none'); }

                        if (currentPage == totalpages - 1) { btnNext.css('display', 'none'); }
                        else btnNext.css('display', '');
                        if (totalpages > settings.length && currentPage < (totalpages - (settings.length / 2)) - 1) { btnLast.css('display', ''); }
                        else {
                            btnLast.css('display', 'none');
                        }
                    }
                    else {
                        btnFirst.css('display', 'none');
                        btnPrev.css('display', 'none');
                        btnNext.css('display', 'none');
                        btnLast.css('display', 'none');
                    }
                }
                function isTextSelected(el) {
                    var startPos = el.get(0).selectionStart;
                    var endPos = el.get(0).selectionEnd;
                    var doc = document.selection;
                    if ((doc && doc.createRange().text.length != 0) ||
                        (!doc && el.val().substring(startPos, endPos).length != 0)) {
                        return true;
                    }
                    return false;
                }
            });
        }
    });
})(jQuery);