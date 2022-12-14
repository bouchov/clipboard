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
        this.mode = 'OWNER'
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
        if (this.mode === 'OWNER') {
            this.account = account;
            localStorage.setItem(this.KEY_USER, JSON.stringify(account));
            this.device = device;
            localStorage.setItem(this.KEY_DEVICE, device);
        } else {
            this.reset()
            this.device = device;
        }
    }

    showApplicationForm() {
        this.showDeviceInfo();
        clipboardWindow.show()
    }

    doReload() {
        this.initApplication()
    }

    initApplication() {
        let form = this;
        let xhttp = new XMLHttpRequest()
        xhttp.onreadystatechange = function () {
            if (this.status === 200) {
                form.log.log('WEB: <<< ' + this.responseText)
                let response = JSON.parse(this.responseText)
                if (response.account) {
                    form.mode = 'OWNER'
                    form.log.log('user is logged in', response.account)
                } else {
                    form.mode = 'GUEST'
                    form.log.log('entered anonymous user', response.device)
                }
                loginWindow.onLoginSuccess(response)
                form.showApplicationForm()
            } else {
                form.log.warn('User is not logged in')
                form.reset()
                form.showDeviceInfo()
                loginWindow.show()
            }
        };
        this.sendGet(xhttp, '/ping')
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
        if (account) {
            this.saveLogin(account.name)
        }
        deviceInfo.saveAccount(account, response.device)
        clipboardWindow.saveContents(response.contents)
    }

    saveLogin(login) {
        localStorage.setItem(this.KEY_LOGIN, login);
        this.userName.value = login;
    }

    beforeShow() {
        enableAllButtons(this.element)
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
        this.doLoginOrRegister(false)
    }

    doRegister() {
        this.doLoginOrRegister(true)
    }

    doLoginOrRegister(doRegister) {
        if (this.userName.value == null || this.userName.value === ''
            || this.userPassword.value == null || this.userPassword.value === '') {
            return false
        }
        disableAllButtons(this.element)
        let login = this.userName.value
        let password = this.userPassword.value
        this.userPassword.value = ''
        if (doRegister) {
            this.doRegisterRequest(login, password)
        } else {
            this.doLoginRequest(login, password)
        }
    }

    doLoginRequest(login, password) {
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
                form.log.warn('Need ReLogin: ' + this.status)
                form.userPassword.value = password;
                messageWindow.showMessage('Произошла ошибка. Попробуйте ещё раз.',
                    () => {form.show()})
            } else {
                form.log.warn('Login Failed: ' + this.status);
                messageWindow.showMessage(this.responseText,
                    () => {form.show()})
            }
        };
        if (deviceInfo.device) {
            this.sendPost(xhttp, '/', 'name=' + login + '&password=' + password +
                '&device=' + deviceInfo.device)
        } else {
            this.sendPost(xhttp, '/', 'name=' + login + '&password=' + password)
        }
    }

    doRegisterRequest(login, password) {
        let form = this
        let xhttp = new XMLHttpRequest()
        xhttp.onreadystatechange = function () {
            form.hide();
            if (this.status === 200) {
                form.log.log('WEB: <<< ' + xhttp.responseText)
                let response = JSON.parse(this.responseText)
                form.log.log('Signed-In Successfully')
                form.onLoginSuccess(response)
                form.runCallback()
            } else if (this.status === 409) {
                form.log.warn('User already exists')
                form.userPassword.value = password;
                messageWindow.showMessage('Произошла ошибка. Попробуйте другой логин.',
                    () => {form.show()})
            } else {
                form.log.warn('Registration Failed: ' + this.status);
                messageWindow.showMessage(this.responseText,
                    () => {form.show()})
            }
        };
        this.sendPost(xhttp, '/register', 'name=' + login + '&password=' + password)
    }

    doLogout() {
        this.log.log("LOGOUT")
        let form = this
        let xhttp = new XMLHttpRequest()
        xhttp.onreadystatechange = function () {
            if (this.status === 200) {
                form.log.log('Signed-Out Successfully')
                location.reload()
            }
        };
        this.sendGet(xhttp, '/logout')
    }
}

class ClipboardWindow extends WebForm {
    BUFFER_SIZE = 4096

    constructor() {
        super('clipboardWindow')
        this.element = document.getElementById('clipboardWindow')
        this.menuBar = document.getElementById('clipboardWindow-menuBar')
        this.textVew = document.getElementById('clipboardWindow-textView')
        this.textarea = document.getElementById('clipboardWindow-text')
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

    beforeShow() {
        this.textVew.style.display = 'none'
        this.downloadView.style.display = 'none'
        this.uploadView.style.display = 'none'
        if (deviceInfo.mode === 'GUEST') {
            disableAllButtons(this.element)
            this.textarea.disabled = true
            forElementsTree(this.menuBar, 'SPAN', span => {
                if (span.title !== 'Close') {
                    span.style.display = 'none'
                }
            })
        } else {
            let disabledShare = this.contents.length === 0
            forInputsTree(this.element, 'button', input => {
                if (input.name === 'share') {
                    input.disabled = disabledShare
                }
            })
        }
        if (this.currentView === this.textVew) {
            this.textVew.style.display = 'block';
            this.log.log('show text view')
        }
        if (this.currentView === this.downloadView) {
            enableAllButtons(this.element)
            this.downloadView.style.display = 'block'
            this.log.log('show download view')
        }
        if (this.currentView === this.uploadView) {
            this.uploadView.style.display = 'block'
            this.log.log('show upload view')
        }
        return super.beforeShow()
    }

    connect(device, target, callback) {
        if (!this.webSocket || this.webSocket.readyState !== WebSocket.OPEN) {
            let url = getWebsocketProtocol() + '://' + getUrl() + '/websocket';
            this.log.log('WEBSOCKET: connecting to ', url)
            this.welcomeCallback = callback
            let form = this;
            this.webSocket = new WebSocket(url);
            this.webSocket.binaryType = "arraybuffer";
            this.webSocket.onopen = function () {
                form.log.log('WEBSOCKET: connected to ', url, 'sending handshake')
                form.sendWebSocket(JSON.stringify({enter: {device: device, target: target}}));
            }
            this.webSocket.onmessage = clipboardWebsocketMessageHandler;
            this.webSocket.onclose = function (event) {
                form.log.log('WEBSOCKET: connection closed by server', event);
                if (event.code === 4004) {
                    form.log.log('WEBSOCKET: session expired');
                    messageWindow.showMessage('Сессия истекла',
                        () => {loginWindow.show()})
                } else if (event.code === 4001) {
                    form.log.log('WEBSOCKET: invalid device');
                    messageWindow.showMessage('Данные устарели')
                } else {
                    form.log.log('WEBSOCKET: reconnecting');
                    setTimeout(() => {
                        form.connect(device, target, callback)
                    }, 5000)
                }
            }
            this.webSocket.onerror = function(err) {
                form.log.warn('WEBSOCKET: error occur', err, 'Close socket')
                form.webSocket.close()
            }
        }
    }

    close() {
        if (this.webSocket && this.webSocket.readyState === WebSocket.OPEN) {
            this.webSocket.onclose = null;
            this.webSocket.close();
            this.log.log("WEBSOCKET: close connection");
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
        } else if (message.errorCode) {
            this.log.warn('error received: ', message);
            let form = this;
            if (message.errorCode === 4) {
                this.log.warn('schedule download request in 5 sec')
                setTimeout(() => {
                    form.downloadSlice()
                }, 5000)
            } else if (message.errorCode === 404) {
                let idx = this.blobs.length - 1
                if (idx >= 0) {
                    this.blobs[idx].status = 2
                    this.log.warn('skip downloading ' + this.blobs[idx].name + ', move to next file')
                }
                messageWindow.showMessage(message.errorMessage, () => {
                    form.show()
                    form.downloadSlice()
                })
            } else {
                messageWindow.showMessage(message.errorMessage)
            }
        }
    }

    onWebsocketArrayBuffer(message) {
        this.log.log('received array buffer[' + message.byteLength + ']')
        this.saveSlice(message)
    }

    onInputFileChanged() {
        let diff = this.isFileContentIsDiffer(this.contents)
        this.blobs = []
        this.file = undefined
        if (diff && this.contents.length > 0) {
            this.doClear()
        }
    }

    isFileContentIsDiffer(contents) {
        if (this.inputFile.files.length !== contents.length) {
            return true
        } else {
            for (let i=0; i < this.inputFile.files.length; i++) {
                let file = this.inputFile.files[i]
                if (file.name !== contents[i].data.name) {
                    return  true
                }
            }
        }
        return false
    }

    showUploadFilesView() {
        if (this.currentView !== this.uploadView) {
            this.hide()
            this.currentView = this.uploadView
            this.show()
        }
    }

    showTextView() {
        if (this.currentView !== this.textVew) {
            this.hide()
            this.currentView = this.textVew
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
        this.link = {token: undefined}
        if (this.contents.length === 0) {
            if (this.currentView === this.uploadView) {
                this.downloadFileList.innerHTML = ''
                this.uploadFileList.innerHTML = ''
                this.file = undefined
            } else {
                this.currentView = this.textVew;
                this.textarea.value = '';
            }
            this.close()
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
                    this.file = undefined
                    if (!this.isFileContentIsDiffer(this.contents)) {
                        this.contents.forEach(function(content, idx) {
                            form.writeUploadRecord(form.uploadFileList, idx, content)
                        })
                        this.writeShareButton(this.uploadFileList)
                        this.connect(deviceInfo.device)
                    } else {
                        this.log.warn('Contents are differ: must clean clipboard')
                    }
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

    writeShareButton(element) {
        element.insertAdjacentHTML('beforeend',
            '<div class="table-row">' +
            '  <div class="table-cell" style="width: 100%">' +
            '    <div class="table-cell-content">' +
            '      <button type="button" class="dialog-button" name="share" onclick="doClickShareButton()">Поделиться</button>' +
            '    </div>' +
            '  </div>' +
            '</div>'
            )
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
            if (data !== '') {
                records = [{type: 'CLIPBOARD', data: data}]
            }
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
            this.sendRecords(records)
        } else {
            log.warn("cannot send empty records")
        }
    }

    sendRecords(records) {
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
                messageWindow.showMessage(this.responseText,
                    () => {form.show()})
            }
        };
        this.sendJson(xhttp, '/clipboard', records);
    }

    doClear() {
        this.sendRecords([])
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
        if (blob === undefined || blob.status || content === undefined) {
            if (blob && blob.status) {
                this.generateDownloadLink(idx, blob)
            }
            do {
                idx++
                if (idx === this.contents.length) {
                    this.generateDownloadAllLink()
                    return
                }
                content = this.contents[idx]
                blob = {
                    buffer: [],
                    pos: 0,
                    status: 0,
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
            if (blob.pos >= blob.size) {
                blob.status = 1; //finished
            }
            this.showProgress(idx, blob.pos, blob.size)
            //next page
            this.downloadSlice()
        }
    }

    generateDownloadAllLink() {
        if (this.createZipFile()) {
            this.downloadFileList.insertAdjacentHTML('beforeend',
                '<div class="table-row">' +
                '  <div class="table-cell" style="width: 100%">' +
                '    <div class="table-cell-content">' +
                '      <a href="#" onclick="return doDownloadAllFiles()">Скачать все файлы</a>' +
                '    </div>' +
                '  </div>' +
                '</div>'
            )
        }
    }

    generateDownloadLink(idx, blob) {
        if (blob.status === 1) {
            let data = new Blob(blob.buffer, {type: blob.type})
            let url = window.URL.createObjectURL(data)

            let progress = document.getElementById('clipboardWindow-fileProgress' + idx)
            let parent = progress.parentElement
            parent.removeChild(progress)
            let href = document.createElement('A')
            parent.appendChild(href)
            href.href = url
            href.download = blob.name
            href.innerText = 'Скачать'
        } else if (blob.status === 2) {
            let progress = document.getElementById('clipboardWindow-fileProgress' + idx)
            let parent = progress.parentElement
            parent.removeChild(progress)
            let p = document.createElement('P')
            parent.appendChild(p)
            p.innerText = 'Ошибка'
        }
    }

    createZipFile() {
        try {
            return new JSZip()
        } catch (e) {
            this.log.warn('JSZip is not available')
        }
    }

    downloadAllFiles() {
        let zip = this.createZipFile()
        this.blobs.forEach((blob, idx) => {
            let data = new Blob(blob.buffer, {type: blob.type})
            let content = this.contents[idx]
            zip.add(blob.name, data, {binary: true, date: content.data.lastModified})
        })
        let data = zip.generate(true)

        let href = document.createElement('A')
        href.style.display = 'none'
        let url = window.URL.createObjectURL(data)
        href.href = url
        href.download = 'clipboard.zip'
        href.innerText = 'Скачать'
        this.element.appendChild(href)
        href.click()
        this.element.removeChild(href)
        window.URL.revokeObjectURL(url)
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
            this.log.warn("file not found: ", slice.name)
            this.sendWebSocket(JSON.stringify(
                {recipient: slice.client,
                    message: {
                        errorCode: 404,
                        errorMessage: 'File not found ' + slice.name,
                        client: deviceInfo.device,
                        name: slice.name
                    }
                }));
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
        if (this.contents.length !== 0) {
            let form = this;
            if (this.link.token) {
                form.hide()
                messageWindow.showMessage(this.generateShareLink(this.link),
                    () => {form.show()})
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
                            () => {form.show()})
                    } else if (this.status === 409) {
                        form.log.warn('Cannot share. Your are not the owner of content')
                        messageWindow.showMessage('Произошла ошибка. Можно поделиться только своими данными.',
                            () => {form.show()})
                    } else if (this.status === 404) {
                        form.log.warn('Cannot share. Empty clipboard')
                        messageWindow.showMessage('Произошла ошибка. Можно поделиться только своими данными.',
                            () => {form.show()})
                    } else {
                        form.log.warn('error share content: ', xhttp.responseText);
                        messageWindow.showMessage(this.responseText,
                            () => {form.show()})
                    }
                };
                this.sendPost(xhttp, '/share');
            }
        }
    }

    generateShareLink(link) {
        if (!this.anchor) {
            this.anchor = document.createElement('A')
        }
        this.anchor.href = '/share/' + link.token
        return '<textarea style="width: 100%; resize: none" rows="1" disabled>' + this.anchor.href + '</textarea>'
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

function doChangeInputFile(event) {
    clipboardWindow.onInputFileChanged()
}

function doDownloadAllFiles(event) {
    clipboardWindow.downloadAllFiles()
    return false
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