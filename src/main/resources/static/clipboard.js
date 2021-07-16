class MessageWindow extends WebForm {
    constructor() {
        super('messageWindow')
        this.text = document.getElementById('messageWindow-text')
        this.submit = document.getElementById('messageWindow-submit')
        this.addSubmitListener(doHideMessageWindow)
    }

    showMessage(text, callback) {
        this.text.innerHTML = text;
        if (callback === undefined) {
            callback = function () {clipboardWindow.show()}
        }
        this.show(callback);
    }

    show(callback) {
        super.show(callback);
        this.submit.focus();
    }

    dismiss() {
        if (this.hide()) {
            this.runCallback();
        }
    }
}

class LoadingWindow extends WebForm {
    constructor() {
        super('loadingWindow');
    }

    show(callback) {
        this.element.style.display='block';
    }

    hide() {
        this.element.style.display='none';
        return true;
    }
}

class DeviceInfo extends WebForm {
    KEY_USER = 'KEY_USER'
    KEY_DEVICE = 'KEY_DEVICE'

    constructor() {
        super('deviceInfo');

        this.nameElement = document.getElementById('deviceInfo-name')

        let jsonUser = localStorage.getItem(this.KEY_USER)
        if (!jsonUser) {
            this.account = {name: 'Гость'}
        } else {
            this.account = JSON.parse(jsonUser)
        }
        this.device = localStorage.getItem(this.KEY_DEVICE)
        if (!this.device) {
            this.device = undefined
        }
        this.showDeviceInfo();
    }

    reset() {
        this.account = {name: 'Гость'}
        this.device = undefined
    }

    showDeviceInfo() {
        if (!this.device) {
            this.nameElement.innerHTML = '<p>' + this.account.name + '</p>';
        } else {
            this.nameElement.innerHTML = '<p>' + this.account.name + ': ' + this.device + '</p>';
        }
    }

    saveAccount(account, device) {
        this.account = account;
        localStorage.setItem(this.KEY_USER, JSON.stringify(account));
        this.device = device;
        localStorage.setItem(this.KEY_DEVICE, device);
    }

    showApplicationForm() {
        this.showDeviceInfo();
        clipboardWindow.show()
    }

    initApplication() {
        if (this.account.id) {
            let form = this;
            let xhttp = new XMLHttpRequest();
            xhttp.onreadystatechange = function () {
                if (this.readyState === 4) {
                    if (this.status === 200) {
                        form.log.log('WEB: <<< ' + xhttp.responseText);
                        let response = JSON.parse(this.responseText);
                        form.log.log('user is logged in', response.account);
                        loginWindow.onLoginSuccess(response);
                        form.showApplicationForm();
                    } else {
                        form.log.warn('Session expired');
                        form.reset()
                        form.showDeviceInfo()
                        loginWindow.show();
                    }
                }
            };
            this.sendGet(xhttp, '/ping');
        } else {
            this.log.log('user is not logged in')
            loginWindow.show();
        }
    }

    show() {
        //always shown
    }


    hide() {
        return false;
    }
}

class LoginWindow extends WebForm {
    KEY_LOGIN = 'KEY_LOGIN'

    constructor() {
        super('loginWindow');
        this.element = document.getElementById('loginWindow');
        this.userName = document.getElementById('loginWindow-name');
        this.userPassword = document.getElementById('loginWindow-password');
        this.submitButton = document.getElementById('loginWindow-submit');

        this.addSubmitListener(doSubmitLoginWindow)

        let login = localStorage.getItem(this.KEY_LOGIN);
        if (!login) {
            this.userName.value = '';
        } else {
            this.userName.value = login;
        }
    }

    onLoginSuccess(response) {
        let account = response.account
        this.saveLogin(account.name)
        deviceInfo.saveAccount(account, response.device)
        clipboardWindow.saveContents(response.contents)
    }

    saveLogin(login) {
        localStorage.setItem(this.KEY_LOGIN, login);
        this.userName.value = login;
    }

    beforeShow() {
        this.submitButton.disabled = false;
        return super.beforeShow();
    }


    show(callback) {
        if (callback === undefined) {
            callback = function () {deviceInfo.showApplicationForm()};
        }
        super.show(callback);
        if (!this.userName.value) {
            this.userName.focus();
        } else {
            this.userPassword.focus();
        }
    }

    doSubmit() {
        if (this.userName.value == null || this.userName.value === ''
            || this.userPassword.value == null || this.userPassword.value === '') {
            return false
        }
        this.submitButton.disabled = true
        let login = this.userName.value
        let password = this.userPassword.value
        this.userPassword.value = ''
        let form = this
        let xhttp = new XMLHttpRequest()
        xhttp.onreadystatechange = function () {
            form.hide();
            if (this.status === 200) {
                form.log.log('WEB: <<< ' + xhttp.responseText)
                let response = JSON.parse(this.responseText)
                form.log.log('Logged-In Successfully')
                form.onLoginSuccess(response)
                form.runCallback()
            } else if (this.status === 409) {
                //need re-login
                form.log.warn('Need ReLogin: ' + this.status)
                form.userPassword.value = password;
                messageWindow.showMessage('Произошла ошибка. Попробуйте ещё раз.', function() {form.show()})
            } else {
                form.log.warn('Login Failed: ' + this.status);
                messageWindow.showMessage(this.responseText, function() {form.show()})
            }
        };
        if (deviceInfo.device) {
            this.sendPost(xhttp, '/', 'name=' + login + '&password=' + password +
                '&device=' + deviceInfo.device)
        } else {
            this.sendPost(xhttp, '/', 'name=' + login + '&password=' + password)
        }
    }
}

class ClipboardWindow extends WebForm {
    constructor() {
        super('clipboardWindow')
        this.element = document.getElementById('clipboardWindow')
        this.textVew = document.getElementById('clipboardWindow-textView')
        this.textarea = document.getElementById('clipboardWindow-text')
        this.shareButton = document.getElementById('clipboardWindow-share')
        this.shareButton.addEventListener('click', doClickShareButton)
        this.downloadView = document.getElementById('clipboardWindow-downloadView')
        this.uploadView = document.getElementById('clipboardWindow-uploadView')

        this.webSocket = undefined

        this.addSubmitListener(doSubmitClipboard)

        this.contents = []
        this.anchor = undefined
        this.link = {token: undefined}
        this.currentView = this.textVew
    }

    connect(device) {
        let url = getWebsocketProtocol() + '://' + getUrl() + '/websocket';
        this.webSocket = new WebSocket(url);
        let form = this;
        this.webSocket.onopen = function () {
            form.webSocket.send(JSON.stringify({enter: {device: device}}));
        }
        this.webSocket.onmessage = clipboardWebsocketMessageHandler;
        this.webSocket.onclose = function () {
            form.log.log("connection closed by server");
        }
    }

    close() {
        if (this.webSocket && this.webSocket.readyState === WebSocket.OPEN) {
            this.webSocket.onclose = null;
            this.webSocket.close();
            this.webSocket = null;
            this.log.log("close connection");
        }
    }

    onWebsocketJsonMessage(message) {
        this.log.log('received message:', message)
    }

    onWebsocketArrayBuffer(message) {
        this.log.log('received array buffer[' + message.byteLength + ']')
    }

    showUploadFilesView() {
        if (this.currentView !== this.uploadView) {
            this.hide()
            this.currentView = this.uploadView
            this.show()
        }
    }

    setLink(link) {
        this.link = link
    }

    saveContents(contents) {
        if (!contents) {
            this.contents = []
        } else {
            this.contents = contents;
        }
        if (this.contents.length === 0) {
            this.currentView = this.textVew;
            this.textarea.value = '';
        } else {
            let form = this
            this.contents.forEach(function(content) {
                if (content.type === 'CLIPBOARD') {
                    form.currentView = form.textVew;
                    form.textarea.value = content.data;
                }
            })
        }
    }

    beforeShow() {
        this.textVew.style.display = 'none';
        this.downloadView.style.display = 'none';
        this.uploadView.style.display = 'none';
        if (this.currentView === this.textVew) {
            this.shareButton.disabled = this.contents.length !== 1;
            this.textVew.style.display = 'block';
        }
        if (this.currentView === this.downloadView) {
            this.downloadView.style.display = 'block';
        }
        if (this.currentView === this.uploadView) {
            this.uploadView.style.display = 'block';
        }
        return super.beforeShow();
    }

    doSubmit() {
        if (this.currentView === this.textVew) {
            let form = this;
            let xhttp = new XMLHttpRequest();

            xhttp.onreadystatechange = function () {
                form.hide()
                if (this.status === 200) {
                    form.log.log('WEB: <<< ' + xhttp.responseText);
                    let contents = JSON.parse(xhttp.responseText);
                    form.saveContents(contents)
                    form.show()
                } else {
                    form.log.warn('error saving content: ', xhttp.responseText);
                    messageWindow.showMessage(this.responseText, function() {form.show()})
                }
            };
            let data = this.textarea.value
            this.sendJson(xhttp, '/clipboard', [{data: data, type: 'CLIPBOARD'}]);
        }
    }

    doShare() {
        if (this.currentView === this.textVew) {
            if (this.contents.length === 1) {
                let form = this;
                if (this.link.token) {
                    form.hide()
                    messageWindow.showMessage(this.generateShareLink(this.link),
                        function() {form.show()})
                } else {
                    let form = this;
                    let xhttp = new XMLHttpRequest();

                    xhttp.onreadystatechange = function () {
                        form.hide()
                        if (this.status === 200) {
                            form.log.log('WEB: <<< ' + xhttp.responseText);
                            let link = JSON.parse(xhttp.responseText);
                            form.setLink(link)
                            messageWindow.showMessage(form.generateShareLink(link),
                                function() {form.show()})
                        } else {
                            form.log.warn('error saving content: ', xhttp.responseText);
                            messageWindow.showMessage(this.responseText, function() {form.show()})
                        }
                    };
                    this.sendGet(xhttp, '/share');
                }
            }
        }
    }

    generateShareLink(link) {
        if (!this.anchor) {
            this.anchor = document.createElement('A')
        }
        this.anchor.href = '/shared/' + link.token
        return '<A href="' + this.anchor.href + '">Ссылка</A>'
    }
}

var loadingWindow = new LoadingWindow()
var messageWindow = new MessageWindow()
var loginWindow = new LoginWindow()
var clipboardWindow = new ClipboardWindow()
var deviceInfo = new DeviceInfo()


//callbacks
function doHideMessageWindow(event) {
    messageWindow.dismiss()
}

function doSubmitLoginWindow(event) {
    loginWindow.doSubmit()
}

function doSubmitClipboard(event) {
    clipboardWindow.doSubmit()
}

function doClickShareButton(event) {
    clipboardWindow.doShare()
}

function clipboardWebsocketMessageHandler(event) {
    log.log('WEBSOCKET: <<< ', event.data);
    if (typeof event.data == 'string') {
        let msg = JSON.parse(event.data);
        clipboardWindow.onWebsocketJsonMessage(msg)
    } else if (event.data instanceof "arraybuffer") {
        clipboardWindow.onWebsocketArrayBuffer(event.data)
    }
}