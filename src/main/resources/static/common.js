class Log {
    constructor(name) {
        this.name = name;
    }

    log(...message) {
        if (!this.name) {
            console.log(message);
        } else {
            console.log(this.name, message);
        }
    }

    warn(...message) {
        if (!this.name) {
            console.warn(message);
        } else {
            console.warn(this.name, message);
        }
    }
}

var log = new Log();
var modalWindow;

class HttpCommunicator {
    constructor(name) {
        this.log = new Log(name);
    }

    overrideRequest(xhttp) {
        let callback = xhttp.onreadystatechange;
        xhttp.onreadystatechange = function () {
            if (this.readyState === 4) {
                loadingWindow.hide();
                if (callback) {
                    callback.call(xhttp);
                }
            }
        }
        loadingWindow.show();
    }

    sendJson(xhttp, url, value) {
        this.overrideRequest(xhttp);
        xhttp.open('POST', url, true);
        xhttp.setRequestHeader('Content-type', 'application/json');
        if (value !== undefined) {
            let body = JSON.stringify(value);
            this.log.log('POST', url, body)
            xhttp.send(body);
        } else {
            this.log.log('POST', url)
            xhttp.send();
        }
    }

    sendPost(xhttp, url, content) {
        this.overrideRequest(xhttp);
        xhttp.open('POST', url, true);
        xhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
        if (content !== undefined) {
            this.log.log('POST', url, content)
            xhttp.send(content);
        } else {
            this.log.log('POST', url)
            xhttp.send();
        }
    }

    sendGet(xhttp, url) {
        this.overrideRequest(xhttp);
        xhttp.open('GET', url, true);
        this.log.log('GET', url)
        xhttp.send();
    }
}

class WebForm extends HttpCommunicator {
    constructor(name) {
        super(name);
        this.name = name;
        this.log = new Log(name);
        this.element = document.getElementById(name);

        this.callback = undefined;
    }

    addSubmitListener(listener) {
        let form = findForm(this.element);
        form.addEventListener('submit', function (event) {
            event.preventDefault()
            listener(event)
        })
    }

    beforeShow() {
        this.log.log('beforeShow');
        return true;
    }

    show(callback) {
        if (callback !== undefined) {
            this.callback = callback;
        }
        if (this.beforeShow()) {
            if (modalWindow !== undefined) {
                modalWindow.hide();
            }
            modalWindow = this;
            this.element.style.display='block';
        }
    }

    hide() {
        if (modalWindow === this) {
            this.log.log('hide');
            modalWindow = undefined;
            this.element.style.display='none';
            return true;
        }
        return false;
    }

    runCallback() {
        if (this.callback !== undefined) {
            this.callback();
            this.callback = undefined;
        }
    }

    isShown() {
        return this.element.style.display === 'block';
    }

}

class WebControl extends HttpCommunicator {
    constructor(name) {
        super(name)
        this.control = document.getElementById(name);
        this.log = new Log(name);
        this.loaded = false;
    }

    addListener(event, listener) {
        this.control.addEventListener(event, listener)
    }

    load() {
        if (!this.loaded) {
            this.log.log('loadData');
            this.loadData();
        }
        this.loaded = true;
    }

    reset() {
        this.loaded = false;
    }

    loadData() {
    }
}

class PagedWebForm extends WebForm {
    constructor(id) {
        super(id);
        this.nextPage = document.getElementById(id + '-nextPage');
        this.prevPage = document.getElementById(id + '-prevPage');
        this.submit = document.getElementById(id + '-submit');

        this.pageNumber = 0;
        this.total = undefined;
        this.pageSize = 10;
    }

    reset() {
        this.pageNumber = 0;
        this.total = undefined;
    }

    beforeShow() {
        if (this.pageNumber <= 0) {
            this.prevPage.disabled = true;
        } else {
            this.prevPage.disabled = false;
        }
        if (this.total !== undefined) {
            if (this.submit) {
                this.submit.disabled = false;
            }
            if (this.pageNumber + 1 >= this.total) {
                this.nextPage.disabled = true;
            } else {
                this.nextPage.disabled = false;
            }
        } else {
            if (this.submit) {
                this.submit.disabled = true;
            }
            this.nextPage.disabled = true;
            this.loadPage();
            return false;
        }
        return super.beforeShow();
    }

    loadPage() {
    }

    loadNextPage(inc) {
        let nextPage = this.pageNumber + inc;
        if (nextPage >= 0) {
            if (this.total !== undefined) {
                if (nextPage < this.total) {
                    this.pageNumber = nextPage;
                    this.loadPage();
                }
            }
        }
    }
}

function playSound(audio) {
    audio.volume = 0.2;
    audio.play();
}

function disableAllButtons(element) {
    changeAllButtons(element, true)
}

function enableAllButtons(element) {
    changeAllButtons(element, false)
}

function findForm(element) {
    let node = element.firstChild
    do {
        if (node.tagName === 'FORM') {
            return node
        }
        node = node.nextSibling
    } while (node)
    return undefined
}

function changeAllButtons(element, disabled) {
    let node = element.firstChild;
    do {
        if (node.tagName === 'BUTTON') {
            node.disabled = disabled;
        }
        node = node.nextSibling;
    } while (node);
}

function onSelectAll(id) {
    forInputs(id, 'checkbox', function(checkbox) {checkbox.checked = true})
}

function onInvertAll(id) {
    forInputs(id, 'checkbox', function(checkbox) {checkbox.checked = !checkbox.checked})
}

function forInputs(id, inputType, callback) {
    let element = document.getElementById(id);
    let node = element.firstChild;
    do {
        if (node.tagName === 'FORM') {
            let form = node;
            for (let i = 0; i < form.length; i++) {
                if (form[i].type === inputType) {
                    let input = form[i];
                    callback(input);
                }
            }
        }
        node = node.nextSibling;
    } while (node);
}


function setLabelClass(input, labelClass) {
    if (input.labels) {
        input.labels.forEach(function(label) {label.className=labelClass})
    }
}

function goTo(url) {
    document.location.href = url;
}

function getWebsocketProtocol() {
    let protocol = window.location.protocol;
    if (protocol.startsWith('https')) {
        return 'wss';
    } else {
        return 'ws';
    }
}

function getUrl() {
    let url = window.location.hostname;
    if (window.location.port) {
        url += ':' + window.location.port;
    }
    return url;
}
