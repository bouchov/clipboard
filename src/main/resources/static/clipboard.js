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
    BUFFER_SIZE = 4096

    constructor() {
        super('clipboardWindow')
        this.element = document.getElementById('clipboardWindow')
        this.textVew = document.getElementById('clipboardWindow-textView')
        this.textarea = document.getElementById('clipboardWindow-text')
        this.shareButton = document.getElementById('clipboardWindow-share')
        this.shareButton.addEventListener('click', doClickShareButton)
        this.downloadView = document.getElementById('clipboardWindow-downloadView')
        this.downloadFileList = document.getElementById("clipboardWindow-downloadFileList")
        this.uploadView = document.getElementById('clipboardWindow-uploadView')
        this.inputFile = document.getElementById("clipboardWindow-uploadFile")
        this.inputFile.addEventListener('change', doChangeInputFile)
        this.uploadFileList = document.getElementById("clipboardWindow-uploadFileList")

        this.webSocket = undefined

        this.addSubmitListener(doSubmitClipboard)

        this.contents = []
        this.blobs = []
        this.file = undefined
        this.welcomeCallback = undefined
        this.anchor = undefined
        this.link = {token: undefined}
        this.currentView = this.textVew
        this.downloadFileList.innerHTML = ''
        this.uploadFileList.innerHTML = ''
    }

    connect(device, target, callback) {
        if (!this.webSocket || this.webSocket.readyState !== WebSocket.OPEN) {
            let url = getWebsocketProtocol() + '://' + getUrl() + '/websocket';
            this.log.log('connecting:', url)
            this.welcomeCallback = callback
            let form = this;
            this.webSocket = new WebSocket(url);
            this.webSocket.binaryType = "arraybuffer";
            this.webSocket.onopen = function () {
                form.sendWebSocket(JSON.stringify({enter: {device: device, target: target}}));
            }
            this.webSocket.onmessage = clipboardWebsocketMessageHandler;
            this.webSocket.onclose = function () {
                form.log.log("connection closed by server");
            }
        }
    }

    close() {
        if (this.webSocket && this.webSocket.readyState === WebSocket.OPEN) {
            this.webSocket.onclose = null;
            this.webSocket.close();
            this.log.log("close connection");
        }
        this.webSocket = undefined;
    }

    sendWebSocket(data) {
        if (typeof data == 'string') {
            this.log.log("WEBSOCKET: >>> ", data)
        } else if (data instanceof ArrayBuffer) {
            this.log.log("WEBSOCKET: >>> ", data.byteLength)
        }
        this.webSocket.send(data)
    }

    onWebsocketJsonMessage(message) {
        this.log.log('received message:', message)
        if (message.welcome) {
            if (this.welcomeCallback) {
                this.welcomeCallback()
                this.welcomeCallback = undefined
            }
        } else if (message.action) {
            if (message.action === 'GET') {
                if (this.currentView === this.uploadView) {
                    this.uploadSlice(message)
                }
            }
        }
    }

    onWebsocketArrayBuffer(message) {
        this.log.log('received array buffer[' + message.byteLength + ']')
        this.saveSlice(message)
    }

    onInputFileChanged() {
        this.contents = []
        this.blobs = []
        this.file = undefined
    }

    showUploadFilesView() {
        if (this.currentView !== this.uploadView) {
            this.hide()
            this.currentView = this.uploadView
            this.link = {token: undefined}
            this.show()
        }
    }

    showTextView() {
        if (this.currentView !== this.textVew) {
            this.hide()
            this.currentView = this.textVew
            this.link = {token: undefined}
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
        this.blobs = []
        if (this.contents.length === 0) {
            this.currentView = this.textVew;
            this.textarea.value = '';
        } else {
            let form = this
            let record = this.contents[0]
            if (record.type === 'CLIPBOARD') {
                form.currentView = form.textVew;
                form.textarea.value = record.data;
                this.close()
            } else if (record.type === 'FILE') {
                if (record.source === deviceInfo.device) {
                    form.currentView = form.uploadView;
                    this.downloadFileList.innerHTML = ''
                    this.uploadFileList.innerHTML = ''
                    this.contents.forEach(function(content, idx) {
                        form.writeUploadRecord(form.uploadFileList, idx, content)
                    })
                    this.file = undefined
                    this.connect(deviceInfo.device)
                } else {
                    form.currentView = form.downloadView;
                    this.downloadFileList.innerHTML = ''
                    this.uploadFileList.innerHTML = ''
                    this.contents.forEach(function(content, idx) {
                        form.writeDownloadRecord(form.downloadFileList, idx, content)
                    })
                    this.close()
                }
            }
        }
    }

    writeUploadRecord(element, idx, record) {
        element.insertAdjacentHTML('beforeend',
            '<div class="table-row">' +
            '  <div class="table-cell">' +
            '    <div class="table-cell-content">' +
            '      <p>' + record.data.name + ' (' + record.data.size + ') ' + record.data.type + '</p>' +
            '    </div>' +
            '  </div>' +
            '  <div class="table-cell" style="width: 10%">' +
            '    <div class="table-cell-content">' +
            '      <progress max="' + record.data.size + '" value="0" id="clipboardWindow-fileProgress' + idx + '">' +
            'загружено <span id="clipboardWindow-filePercent' + idx + '">0</span>%' +
            '      </progress>' +
            '    </div>' +
            '  </div>' +
            '</div>'
        )
    }

    writeDownloadRecord(element, idx, record) {
        element.insertAdjacentHTML('beforeend',
            '<div class="table-row">' +
            '  <div class="table-cell" style="width: 2em">' +
            '    <div class="table-cell-content">' +
            '      <input type="checkbox" name="files" value="'+ idx + '" id="clipboardWindow-download' + idx + '">' +
            '    </div>' +
            '  </div>' +
            '  <div class="table-cell">' +
            '    <div class="table-cell-content">' +
            '      <label for="clipboardWindow-download' + idx + '">' + record.data.name + ' (' + record.data.size + ') ' + record.data.type + '</label>' +
            '    </div>' +
            '  </div>' +
            '  <div class="table-cell" style="width: 10%">' +
            '    <div class="table-cell-content">' +
            '      <progress max="' + record.data.size + '" value="0" id="clipboardWindow-fileProgress' + idx + '">' +
            'скачено <span id="clipboardWindow-filePercent' + idx + '">0</span>%' +
            '      </progress>' +
            '    </div>' +
            '</div>'
        )
    }

    beforeShow() {
        this.textVew.style.display = 'none'
        this.downloadView.style.display = 'none'
        this.uploadView.style.display = 'none'
        if (this.currentView === this.textVew) {
            this.shareButton.disabled = this.contents.length !== 1
            this.textVew.style.display = 'block';
            this.log.log('show text view')
        }
        if (this.currentView === this.downloadView) {
            this.downloadView.style.display = 'block'
            this.log.log('show download view')
        }
        if (this.currentView === this.uploadView) {
            this.uploadView.style.display = 'block'
            this.log.log('show upload view')
        }
        return super.beforeShow()
    }

    doSubmit() {
        if (this.currentView === this.downloadView) {
            this.doSubmitDownload()
        } else {
            this.doSubmitSend()
        }
    }

    doSubmitSend() {
        let records = []
        if (this.currentView === this.textVew) {
            let data = this.textarea.value
            records = [{type: 'CLIPBOARD', data: data}]
        } else if (this.currentView === this.uploadView) {
            if (this.inputFile.files.length > 0) {
                for (let i=0; i < this.inputFile.files.length; i++) {
                    let file = this.inputFile.files[i]
                    records.push({
                        type: 'FILE',
                        data: {
                            name: file.name,
                            size: file.size,
                            type: file.type,
                            lastModified: file.lastModified
                        }
                    })
                }
            }
        }
        if (records.length !== 0) {
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
            this.sendJson(xhttp, '/clipboard', records);
        } else {
            log.warn("cannot send empty records")
        }
    }

    doSubmitDownload() {
        if (this.contents.length === 0) {
            this.log.log('nothing to download')
            return
        }
        let form = this
        this.connect(deviceInfo.device, this.contents[0].source,
            function() {
            form.downloadSlice()
        })
    }

    downloadSlice() {
        let blob = undefined
        let content = undefined
        let idx = this.blobs.length - 1
        if (idx >= 0) {
            blob = this.blobs[idx]
            content = this.contents[idx]
        }
        if (blob === undefined || blob.pos >= blob.size || content === undefined) {
            //next file
            do {
                idx++
                if (idx === this.contents.length) {
                    this.generateDownloadLink()
                    return
                }
                content = this.contents[idx]
                blob = {
                    buffer: [],
                    pos: 0,
                    name: content.data.name,
                    type: content.data.type,
                    size: content.data.size
                }
                this.blobs.push(blob)
            } while (blob.pos >= blob.size)
        }
        this.showProgress(idx, blob.pos, blob.size)
        this.sendWebSocket(JSON.stringify(
            {recipient: content.source,
                message: {
                    action: 'GET',
                    client: deviceInfo.device,
                    name: content.data.name,
                    offset: blob.pos,
                    length: this.BUFFER_SIZE
                }
            }));
    }

    saveSlice(buffer) {
        let idx = this.blobs.length - 1
        if (idx >= 0) {
            let blob = this.blobs[idx]
            blob.buffer.push(buffer)
            blob.pos += buffer.byteLength
            this.showProgress(idx, blob.pos, blob.size)
            //next page
            this.downloadSlice()
        }
    }

    generateDownloadLink() {
        this.blobs.forEach((blob, idx) => {
            let data = new Blob(blob.buffer, {type: blob.type})
            let url = window.URL.createObjectURL(data)
            
            let progress = document.getElementById('clipboardWindow-fileProgress' + idx)
            let parent = progress.parentElement
            parent.removeChild(progress)
            let href = document.createElement('A')
            parent.appendChild(href)
            href.href = url
            href.download = blob.name
            href.innerText = 'download'
        })
    }

    showProgress(idx, value, total) {
        if (idx !== undefined) {
            let progress = document.getElementById('clipboardWindow-fileProgress' + idx)
            progress.value = value
            let percent = document.getElementById('clipboardWindow-filePercent' + idx)
            if (total === undefined) {
                total = this.contents[idx].data.size
            }
            percent.innerText = '' + (total - value) * 100 / total
        }
    }

    uploadSlice(slice) {
        if (this.file) {
            if (this.file.name !== slice.name) {
                this.file = undefined
            }
        }
        if (!this.file) {
            for (let i=0; i < this.inputFile.files.length; i++) {
                let file = this.inputFile.files[i]
                if (file.name === slice.name) {
                    this.file = file;
                    break
                }
            }
        }
        if (!this.file) {
            this.log.warn("file not found " + slice.name)
            //send error
        } else {
            let idx
            for (let i=0; i < this.contents.length; i++) {
                if (this.contents[i].data.name === slice.name) {
                    idx = i
                    break
                }
            }
            let chunk = this.file.slice(slice.offset, slice.offset + slice.length)
            let reader = new FileReader()
            let form = this
            reader.onload = function (e) {
                form.log.log('send ' + e.loaded)
                form.showProgress(idx, slice.offset + slice.length)
                form.sendWebSocket(reader.result)
            }
            reader.readAsArrayBuffer(chunk)
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
        return '<A href="' + this.anchor.href + '">Ссылка ' + link.token + '</A>'
    }
}

const loadingWindow = new LoadingWindow()
const messageWindow = new MessageWindow()
const loginWindow = new LoginWindow()
const clipboardWindow = new ClipboardWindow()
const deviceInfo = new DeviceInfo()


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

function doChangeInputFile(event) {
    clipboardWindow.onInputFileChanged()
}

function clipboardWebsocketMessageHandler(event) {
    log.log('WEBSOCKET: <<< ', event.data);
    if (typeof event.data == 'string') {
        let msg = JSON.parse(event.data);
        clipboardWindow.onWebsocketJsonMessage(msg)
    } else if (event.data instanceof ArrayBuffer) {
        clipboardWindow.onWebsocketArrayBuffer(event.data)
    }
}