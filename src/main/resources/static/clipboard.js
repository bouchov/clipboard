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
    KEY_USER = 'KEY_USER';
    KEY_DEVICE = 'KEY_DEVICE';

    constructor() {
        super('deviceInfo');

        this.nameElement = document.getElementById('deviceInfo-name')

        let jsonUser = localStorage.getItem(this.KEY_USER)
        if (!jsonUser) {
            this.account = {name: 'Гость'}
        } else {
            this.account = JSON.parse(jsonUser)
        }
        let jsonDevice = localStorage.getItem(this.KEY_DEVICE)
        if (!jsonDevice) {
            this.device = undefined
        } else {
            this.device = JSON.parse(jsonDevice);
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
            this.nameElement.innerHTML = '<p>' + this.account.name + ': ' + this.device.name + '</p>';
        }
    }

    saveAccount(account) {
        this.account = account;
        localStorage.setItem(this.KEY_USER, JSON.stringify(account));
    }

    saveDevice(device) {
        this.device = device;
        deviceWindow.setDevice(device);
        if (device) {
            localStorage.setItem(this.KEY_DEVICE, JSON.stringify(device));
        }
    }

    showApplicationForm() {
        this.showDeviceInfo();
        if (!this.device) {
            deviceWindow.show()
        } else {
            clipboardWindow.show()
        }
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
        deviceInfo.saveAccount(account)
        deviceInfo.saveDevice(response.device)
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
        this.sendPost(xhttp, '/', 'name=' + login + '&password=' + password)
    }
}

class DeviceControl extends WebControl {
    constructor(name) {
        super(name);
    }

    setDevice(device) {
        if (device) {
            this.control.value = device.token
        } else {
            this.control.value = '0'
        }
    }

    getDeviceToken() {
        return this.control.value;
    }

    loadData() {
        let form = this;
        let xhttp = new XMLHttpRequest();

        xhttp.onreadystatechange = function () {
            if (this.readyState === 4) {
                if (this.status === 200) {
                    form.log.log('WEB: <<< ' + xhttp.responseText);
                    let devices = JSON.parse(xhttp.responseText);
                    form.loadDeviceListData(devices);
                } else {
                    form.reset();
                    form.log.warn('error load categories: ', xhttp.responseText);
                }
            }
        };
        this.sendGet(xhttp, '/devices');
    }

    loadDeviceListData(devices) {
        this.control.innerHTML = '<option disabled selected value="0">Выберите устройство</option>';
        let form = this;
        devices.forEach(function(device){
            form.control.insertAdjacentHTML('beforeend',
                '<option value="' + device.token + '">' + device.name + '</option>');
        });
    }
}

class DeviceWindow extends WebForm {
    constructor() {
        super('deviceWindow');
        this.element = document.getElementById('deviceWindow')
        this.name = document.getElementById('deviceWindow-name')
        this.type = document.getElementById('deviceWindow-type')

        this.devices = new DeviceControl('deviceWindow-select')
        this.devices.addListener('change', doSelectDeviceWindow)
        this.addSubmitListener(doSubmitDeviceWindow)
    }

    reset() {
        this.devices.reset()
    }

    setDevice(device) {
        this.devices.load()
        this.devices.setDevice(device)
    }

    doSelect() {
        let devToken = this.devices.getDeviceToken()
        if (devToken && devToken !== '0') {
            let form = this;
            let xhttp = new XMLHttpRequest();

            xhttp.onreadystatechange = function () {
                if (this.status === 200) {
                    form.log.log('WEB: <<< ' + xhttp.responseText);
                    let device = JSON.parse(xhttp.responseText);
                    deviceInfo.saveDevice(device)
                    deviceInfo.showApplicationForm()
                } else {
                    form.reset();
                    form.log.warn('error saving device: ', xhttp.responseText);
                }
            };
            this.sendPost(xhttp, '/device', 'token=' + devToken);
        }
    }

    doSubmit() {
        let devName = this.name.value
        let devType = this.type.value
        if (devName) {
            let form = this;
            let xhttp = new XMLHttpRequest();

            xhttp.onreadystatechange = function () {
                if (this.status === 200) {
                    form.log.log('WEB: <<< ' + xhttp.responseText);
                    let device = JSON.parse(xhttp.responseText);
                    form.reset()
                    deviceInfo.saveDevice(device)
                    deviceInfo.showApplicationForm()
                } else {
                    form.reset();
                    form.log.warn('error saving device: ', xhttp.responseText);
                }
            };
            this.sendPost(xhttp, '/device', 'name=' + devName + '&type=' + devType);
        }
    }
}

class ClipboardWindow extends WebForm {
    constructor() {
        super('clipboardWindow')
        this.element = document.getElementById('clipboardWindow')
        this.textVew = document.getElementById('clipboardWindow-textView')
        this.textarea = document.getElementById('clipboardWindow-text')
        this.downloadView = document.getElementById('clipboardWindow-downloadView')
        this.uploadView = document.getElementById('clipboardWindow-uploadView')

        this.addSubmitListener(doSubmitClipboard)

        this.contents = []
        this.currentView = this.textVew
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
}

var loadingWindow = new LoadingWindow()
var messageWindow = new MessageWindow()
var loginWindow = new LoginWindow()
var clipboardWindow = new ClipboardWindow()
var deviceInfo = new DeviceInfo()
var deviceWindow = new DeviceWindow()


//callbacks
function doHideMessageWindow(event) {
    messageWindow.dismiss()
}

function doSubmitDeviceWindow(event) {
    deviceWindow.doSubmit()
}

function doSelectDeviceWindow(event) {
    deviceWindow.doSelect()
}

function doSubmitLoginWindow(event) {
    loginWindow.doSubmit()
}

function doSubmitClipboard(event) {
    clipboardWindow.doSubmit()
}