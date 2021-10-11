<template>
    <header class="header">
        <div class="header_content pc">
            <div id="logo">
                <a href="#" class="logo"  v-if="this.$global.projectName === 'mw'">
                    <img src="../../assets/img/logo.svg"/>
                    <div @click="openCosUpgradeDialog()">
                        <span v-if="isUpdate" title="Update" class="update"></span>
                        <span v-if="openApiProxy">MW·<span style="color: #ccc;font-size: smaller;">Light</span></span>
                        <span v-else>MW</span>
                        <span>{{blockchainStatus.application}}{{$t('header.version')}}{{blockchainStatus.fullVersion}}</span>
                    </div>
                </a>
                <a href="#" class="logo" v-else-if="this.$global.projectName === 'sharder'">
                    <img src="../../assets/img/sharder/sharder-logo.svg"/>
                    <div @click="openCosUpgradeDialog()">
                        <span v-if="isUpdate" title="Update" class="update"></span>
                        <span v-if="openApiProxy">Sharder·<span style="color: #ccc;font-size: smaller;">Light</span></span>
                        <span v-else>Sharder</span>
                        <span>{{blockchainStatus.application}}{{$t('header.version')}}{{blockchainStatus.fullVersion}}</span>
                    </div>
                </a>
            </div>
            <nav class="navbar_main" role="navigation">
                <el-menu class="navbar_left el-menu-demo" :class="this.$i18n.locale === 'en'? 'en_menu' : ''"
                         mode="horizontal" :router=isRouter @select="activeItem">
                    <el-menu-item index="/account" :class="this.$route.path.indexOf('/account') >= 0 ? 'activeLi' : ''">
                        {{$t('header.account')}}
                    </el-menu-item>
                    <el-menu-item index="/network" :class="this.$route.path.indexOf('/network') >= 0 ? 'activeLi' : ''">
                        {{$t('header.network')}}
                    </el-menu-item>
<!--                    <el-menu-item index="/mining" :class="this.$route.path.indexOf('/mining') >= 0 ? 'activeLi' : ''">-->
<!--                        {{$t('header.mining')}}-->
<!--                    </el-menu-item>-->
                </el-menu>
                <div class="navbar_console" v-if="this.$global.projectName === 'sharder'">
                    <el-button type="text" @click="goConsole">
                        <span class="console"></span>
                    </el-button>
                </div>
                <div class="navbar_search">
                    <div>
                        <input class="navbar_search_input" :class="activeSearch ? 'navbar_search_input_active' : ''"
                               :placeholder="placeholder" type="text" v-model="search_val"
                               @focus="search_focus" @blur="search_blur" @keyup.enter="search_keydown"/>
                        <img src="../../assets/img/search.svg" @click="search_keydown"/>
                    </div>
                </div>
                <div class="navbar_right">
                    <div class="navbar_status">
                        <span v-if="typeof(secretPhrase) === 'undefined'">{{accountRS}} | {{$t('header.observation_mode')}}</span>
                        <span class="isLogin" v-else>{{accountRS}} | {{$t('header.secret_mode')}}</span>
                    </div>
                    <div class="navbar_pilotLamp">
                        <el-tooltip class="item csp" :content="$t('account.please_init_hub')" placement="bottom"
                                    effect="light" v-if="isHubInit">
                            <div class="pilotLamp_circle notForging"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" :content="$t('header.forging_error_new_account')"
                                    placement="bottom" effect="light"
                                    v-else-if="accountInfo.errorDescription === 'Unknown account'">
                            <div class="pilotLamp_circle notForging"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" :content="$t('header.forging_error_effective_balance')"
                                    placement="bottom" effect="light" v-else-if="accountInfo.effectiveBalanceNQT === 0">
                            <div class="pilotLamp_circle notForging"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" :content="$t('header.forging_error_no_admin_password')"
                                    placement="bottom" effect="light"
                                    v-else-if="typeof(secretPhrase) === 'undefined' && userConfig['sharder.HubBindAddress'] !== accountRS">
                            <div class="pilotLamp_circle unknownForging" @click="startForging(false,'')"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" :content="$t('header.forging_error_exceeds_account_volume')"
                                    placement="bottom" effect="light"
                                    v-else-if="typeof(secretPhrase) !== 'undefined' && userConfig['sharder.HubBindAddress']  !== accountRS">
                            <div class="pilotLamp_circle unknownForging"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" :content="$t('header.no_forging')" placement="bottom"
                                    effect="light" v-else-if="forging.errorCode === 5">
                            <div class="pilotLamp_circle notForging" @click="startForging(true,'')"></div>
                        </el-tooltip>
                        <el-tooltip class="item" :content="$t('header.started_forging')" placement="bottom"
                                    effect="light" v-else-if="!forging.errorDescription">
                            <div class="pilotLamp_circle"></div>
                        </el-tooltip>

                    </div>
                    <div class="navbar_exit">
                        <span class="csp" @click="exit"><a>{{$t('header.exit')}}</a></span>
                    </div>
                    <div class="navbar_lang">
                        <el-select v-model="selectLan">
                            <el-option
                                v-for="item in language"
                                :key="item.value"
                                :label="item.label"
                                :value="item.value">
                            </el-option>
                        </el-select>
                    </div>
                    <div class="navbar_refresh">
                        <el-tooltip class="item csp" :content="$t('header.refresh')" placement="bottom"
                                    effect="light">
                            <i class="el-icon-refresh-right" @click="refreshPage"></i>
                        </el-tooltip>
                    </div>
                </div>
            </nav>
        </div>

        <div class="mobile">
            <div class="navbar">
                <div id="logo_mobile">
                    <a href="#" class="logo">
<!--                        <img src="../../assets/img/logo.svg"/>-->
                        <div @click="openCosUpgradeDialog()">
                            <span v-if="isUpdate" title="Update" class="update"></span>
                            <span>MW</span>
                            <span>{{blockchainStatus.fullVersion}}</span>
                        </div>
                    </a>
                </div>
                <el-menu class="navbar_left el-menu-demo" :class="this.$i18n.locale === 'en'? 'en_menu' : ''"
                         mode="horizontal" :router=isRouter @select="activeItem">
                    <el-menu-item index="/account" :class="this.$route.path.indexOf('/account') >= 0 ? 'activeLi' : ''">
                        {{$t('header.account')}}
                    </el-menu-item>
                    <el-menu-item index="/network" :class="this.$route.path.indexOf('/network') >= 0 ? 'activeLi' : ''">
                        {{$t('header.network')}}
                    </el-menu-item>
                    <!--<el-menu-item index="/mining" :class="this.$route.path.indexOf('/mining') >= 0 ? 'activeLi' : ''">-->
                        <!--{{$t('header.mining')}}-->
                    <!--</el-menu-item>-->
                </el-menu>


                <div class="nav-right">
                    <div class="navbar_lang">
                        <el-select v-model="selectLan">
                            <el-option
                                v-for="item in language"
                                :key="item.value"
                                :label="item.label"
                                :value="item.value">
                            </el-option>
                        </el-select>
                    </div>
                    <div class="navbar_exit">
                        <span class="csp" @click="exit"><a>{{$t('header.exit')}}</a></span>
                    </div>
                    <div class="navbar_refresh">
                        <el-tooltip class="item csp" :content="$t('header.refresh')" placement="bottom"
                                    effect="light">
                            <i class="el-icon-refresh-right" @click="refreshPage"></i>
                        </el-tooltip>
                    </div>
                </div>
            </div>
            <div>
                <div class="navbar_search">
                    <div>
                        <input class="navbar_search_input" :class="activeSearch ? 'navbar_search_input_active' : ''"
                               :placeholder="placeholder" type="text" v-model="search_val"
                               @focus="search_focus" @blur="search_blur" @keyup.enter="search_keydown"/>
                        <img src="../../assets/img/search.svg" @click="search_keydown"/>
                    </div>
                </div>
            </div>
        </div>

        <dialogCommon :searchValue="search_val" :isSearch="isSearch" @isClose="isClose"></dialogCommon>

        <div class="modal" id="start_forging_modal" v-show="startForgingDialog">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog"></button>
                        <h4 class="modal-title">{{$t('header.start_forging')}}</h4>
                    </div>
                    <div class="modal-body modal-peer">
                        <p>{{$t('header.admin_password')}}</p>
                        <input v-model="adminPassword" type="password"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" @click="startForging(false,adminPassword)">
                            {{$t('header.starting_forging')}}
                        </button>
                    </div>
                </div>
            </div>
        </div>
        <div class="download_blocks_loading" v-if="isDownLoadingBlockchain">
            <div class="download_blocks_loading_active" v-show="isDownloadingState === 'isActive'">
                <div>
                    <span>{{$t("account.downloading_blockchain")}}</span>
                    <span v-if="blocksLeft"> ({{blocksLeft + $t("account.remaining_blocks")}})</span>
                    <p v-if="lastBlockHeight">{{$t("account.the_latest_block")}} : {{lastBlockHeight}}</p>
                </div>
                <div class="download_block_progress">
                    <el-progress color="rgba(27,201,142)" :text-inside="true" :stroke-width="18"
                                 :percentage="percentageTotal"></el-progress>
                </div>
            </div>
            <div v-show="isDownloadingState === 'isLightClient'">
                <p>Light Client</p>
                <p>{{$t("account.block_fully_downloaded")}}</p>
            </div>
            <div v-show="isDownloadingState === 'isHalted'">
                <span>{{$t("account.download_no_response")}}</span>
                <span v-if="blocksLeft"> ({{blocksLeft + $t("account.remaining_blocks")}})</span>
                <p v-if="lastBlockHeight">{{$t("account.the_latest_block")}} : {{lastBlockHeight}}</p>
            </div>
        </div>

        <!--view cos upgrade dialog-->
        <div class="modal_cosUpgrade" id="cos_upgrade" v-show="cosUpgradeDialog">
            <div class="modal-header">
                <h4 class="modal-title">
                    <span>{{$t('upgrade.title')}}</span>
                </h4>
            </div>
            <div class="modal-body">
                <div class="version-info">
                    <span>{{$t('upgrade.current_version')}}v{{blockchainStatus.version}}</span>
                    <span style="color: #555;font-style: italic;font-size: smaller;"> {{blockchainStatus.cosLastUpgradeDate}}</span>
                    <span style="color: #ccc;font-style: italic;font-size: smaller;" v-if="openApiProxy">{{$t('sso.light_client')}}</span>
                    <br/>
                    <span v-if="isUpdate">
                        {{$t('upgrade.discover_new_version')}}
                        <span class="found-new-version">{{latestVersion}}</span>
                    </span>
                    <span class="no-new-version" v-else>
                        {{$t('upgrade.no_new_version')}}
                    </span>
                </div>
                <div class="footer-btn">
                    <button class="common_btn writeBtn" @click="closeDialog()">{{$t('upgrade.cancel')}}</button>
                    <button class="common_btn writeBtn" @click="openAdminDialog('update')" v-if="isUpdate">
                        {{$t('upgrade.update')}}
                    </button>
                </div>
            </div>
        </div>

        <AdminPwd :openDialog="adminPasswordDialog" @getPwd="getAdminPassword" @isClose="isClose"></AdminPwd>
    </header>

</template>

<script>

    export default {
        name: "Header",
        props: ["openSidebar", "title"],
        data() {
            return {
                startForgingDialog: false,
                activeIndex: "/account",
                isRouter: true,
                placeholder: this.$t('header.search'),
                activeSearch: false,
                blockchainStatus: [],
                secretPhrase: SSO.secretPhrase,
                adminPassword: '',
                accountRS: SSO.accountRS,
                accountInfo: [],
                forging: [],
                userConfig: [],
                search_val: "",
                isSearch: false,
                isHubInit: this.$store.state.isHubInit,
                selectLan: '',
                selectLanValue: '',
                language: [{
                    value: 'cn',
                    label: '简体中文'
                }, {
                    value: 'en',
                    label: 'English'
                }],
                isDownLoadingBlockchain: SSO.downloadingBlockchain,
                isDownloadingState: SSO.isDownloadingState,
                percentageTotal: SSO.percentageTotal,
                blocksLeft: '',
                lastBlockHeight: '',
                cosUpgradeDialog: false,
                adminPasswordTitle: '',
                adminPasswordDialog: false,
                latestVersion: '',
                upgradeMode: '',
                bakMode: '',
                isUpdate: false,
            };
        },
        created() {
            const _this = this;
            let lang = this.$i18n.locale;
            if (typeof lang !== 'undefined') {

                for (let i = 0; i < _this.language.length; i++) {
                    if (_this.language[i].value === lang) {
                        _this.selectLan = _this.language[i].label;
                        _this.selectLanValue = _this.language[i].value;
                    }
                }
            } else {
                _this.selectLan = _this.language[value === 'cn'].label;
                _this.selectLanValue = _this.language[value === 'cn'].value;
            }
            _this.getState();
            _this.getData();
            _this.getAccountInfo();
            _this.$global.getUserConfig(_this).then(res => {
                _this.userConfig = res;
            });

            // let formData = new FormData();
            // formData.append("secretPhrase", _this.secretPhrase);
            let config = {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            };

            if(SSO.accountInfo.balanceNQT/ 100000000 + SSO.accountInfo.frozenBalanceNQT / 100000000 < 20000){

                _this.$http.post('/sharder?requestType=stopMining', _this.signInfo(_this.secretPhrase), config).then(res => {
                    _this.forging = res.data;
                    // console.log("forging",_this.forging);
                }).catch(err => {
                    console.error(err);
                });
            }

            _this.$http.post('/sharder?requestType=getForging', _this.signInfo(_this.secretPhrase), config).then(res => {
                _this.forging = res.data;
                // console.log("forging",_this.forging);
            }).catch(err => {
                console.error(err);
            });
        },
        mounted() {
            let _this = this;
            setInterval(() => {
                _this.getState();
            }, SSO.downloadingBlockchain ? this.$global.cfg.topSpeedInterval : (this.$global.isOpenApiProxy() ? this.$global.cfg.slowInterval : this.$global.cfg.defaultInterval));
            setInterval(() => {
                _this.getData();
            }, SSO.downloadingBlockchain ? this.$global.cfg.soonInterval : (this.$global.isOpenApiProxy() ? this.$global.cfg.slowInterval : this.$global.cfg.defaultInterval));

            if (/(iPhone|iPad|iPod|iOS|Android)/i.test(navigator.userAgent)) { //移动端
                this.search_focus()
            }
        },
        methods: {
            getAccountInfo:function(){
                const _this = this;
                _this.$http.get(_this.$global.urlPrefix() + "?requestType=getAccount", {
                    params: {
                        includeEffectiveBalance: true,
                        account: SSO.account
                    }
                }).then(res => {
                    _this.accountInfo = res.data;
                }).catch(err => {
                    console.error(err);
                });
            },
            getData: function () {
                const _this = this;
                // if(_this.i%30 === 0){
                // _this.blocksLeft = SSO.blocksLeft;
                //后端此处请求的数据未发送变化
                _this.$global.setBlockchainState(_this).then(res => {
                    _this.blockchainStatus = res.data;

                    // console.log('res.data',res.data)
                    _this.blocksLeft = res.data.lastBlockchainFeederHeight - res.data.lastBlockHeight;
                    _this.percentageTotal =  parseInt(res.data.lastBlockHeight/res.data.lastBlockchainFeederHeight *10000)/100;
                    _this.lastBlockHeight = res.data.lastBlockchainFeederHeight;
                    _this.isDownLoadingBlockchain = res.data.isDownloading;
                    SSO.downloadingBlockchain = res.data.isDownloading;

                    _this.getLatestHubVersion();
                    /*if(_this.$global.isOpenConsole){
                        _this.$global.addToConsole("/sharder?requestType=getBlockchainStatus",'GET',res);
                    }*/
                    // SSO.addToConsole("/sharder?requestType=getBlockchainStatus", 'GET', res.data, res);
                });
                _this.$global.setUnconfirmedTransactions(_this, SSO.account).then(res => {
                    _this.$store.state.unconfirmedTransactionsList = res.data;
                    // console.log("unconfirmedTransactionsList", res.data);
                    /*if(_this.$global.isOpenConsole){
                        _this.$global.addToConsole("/sharder?requestType=getUnconfirmedTransactions",'GET',res);
                    }*/
                    // SSO.addToConsole("/sharder?requestType=getUnconfirmedTransactions", 'GET', res.data, res);
                });
                _this.$global.setPeers(_this).then(res => {
                    /*if(_this.$global.isOpenConsole){
                        _this.$global.addToConsole("/sharder?requestType=getPeers",'GET',res);
                    }*/
                    // SSO.addToConsole("/sharder?requestType=getPeers", 'GET', res.data, res);
                });
            },
            getState: function () {
                const _this = this;
                _this.$global.setBlockchainState(_this).then(res => {
                    _this.blockchainStatus = res.data;
                    SSO.updateBlockchainDownloadProgress();
                    SSO.downloadingBlockchain = _this.blockchainStatus.isDownloading;
                    _this.isDownLoadingBlockchain = _this.blockchainStatus.isDownloading;
                    _this.isDownloadingState = SSO.isDownloadingState;
                    _this.blocksLeft = res.data.lastBlockchainFeederHeight - res.data.lastBlockHeight;
                    _this.percentageTotal =  parseInt(res.data.lastBlockHeight/res.data.lastBlockchainFeederHeight *10000)/100;
                    _this.lastBlockHeight = res.data.lastBlockchainFeederHeight;
                    _this.getLatestHubVersion();
                    /*if(_this.$global.isOpenConsole){
                        _this.$global.addToConsole("/sharder?requestType=getBlockchainStatus",'GET',res);
                    }*/
                    // SSO.addToConsole("/sharder?requestType=getBlockchainStatus", 'GET', res.data, res);
                });
            },
            signInfo: function (secret) {
                let formData = new FormData();
                let timestamp = Date.parse(new Date()).toString();
                let signature = SSO.signBytes(converters.stringToHexString(timestamp), converters.stringToHexString(secret));
                formData.append("signature", signature);
                formData.append("message", timestamp);
                return formData;
            },
            startForging: function (b, pwd) {
                const _this = this;
                let config = {
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                };
                // if (SSO.isPassphraseAtRisk) {
                //     return _this.$message.warning(_this.$t('notification.passphrase_at_risk'));
                // }
                if (b) {
                    if(SSO.accountInfo.balanceNQT/ _this.$global.unitValue + SSO.accountInfo.frozenBalanceNQT / _this.$global.unitValue < 133){
                        return _this.$message.error(_this.$t('notification.ss_not_enough'));
                    }
                    _this.$http.post("/sharder?requestType=startForging", _this.signInfo(SSO.secretPhrase), config).then(res => {
                        if (!res.data.errorDescription) {
                            _this.$http.post('/sharder?requestType=getForging', _this.signInfo(SSO.secretPhrase), config).then(res => {
                                _this.forging = res.data;
                                // console.log("forging",_this.forging);
                            }).catch(err => {
                                console.error(err);
                            });

                        } else {
                            _this.$message.error(res.data.errorDescription);
                            console.error(res.data.errorDescription);
                        }
                    }).catch(err => {
                        console.error(err);
                    });
                } else if (b === false && pwd === '') {
                    _this.startForgingDialog = true;
                    _this.$store.state.mask = true;
                } else {
                    _this.$http.post("/sharder?requestType=startForging", _this.signInfo(pwd), config).then(res => {
                        if (!res.data.errorDescription) {
                            _this.$http.post('/sharder?requestType=getForging', _this.signInfo(pwd), config).then(res => {
                                _this.forging = res.data;
                                // console.log("forging",_this.forging);
                            }).catch(err => {
                                console.error(err);
                            });
                        } else {
                            _this.$message.error(res.data.errorDescription);
                            console.error(res.data.errorDescription);
                        }
                    }).catch(err => {
                        console.error(err);
                    });
                    closeDialog();
                }
            },
            activeItem: function (val) {
                const _this = this;
                _this.activeIndex = val;
            },
            goConsole: function () {
                // const _this = this;

                SSO.showConsole(this);

                /* _this.$global.newConsole = window.open("", "console", "width=750,height=400,menubar=no,scrollbars=yes,status=no,toolbar=no,resizable=yes");
                 $(_this.$global.newConsole.document.head).html("<title>CONSOLE</title><style type='text/css'>body { background:black; color:white; font-family:courier-new,courier;font-size:14px; } pre { font-size:14px; } #console { padding-top:15px; }</style>");
                 $(_this.$global.newConsole.document.body).html("<div style='position:fixed;top:0;left:0;right:0;padding:5px;background:#efefef;color:black;'>"+_this.$t('header.open_console')+"<div style='float:right;text-decoration:underline;color:blue;font-weight:bold;cursor:pointer;' onclick='document.getElementById(\"console\").innerHTML=\"\"'>clear</div></div><div id='console'></div>");
                */
                /* let loop = setInterval(function() {
                     if(_this.$global.newConsole.closed) {
                         clearInterval(loop);
                         _this.$global.isOpenConsole = false;
                     }
                 }, 1000);*/
                // this.$global.isOpenConsole = true;
            },
            search_focus: function () {
                const _this = this;
                _this.activeSearch = true;
                _this.placeholder = _this.$t('header.search_open');
            },
            search_blur: function () {
                const _this = this;
                if (_this.search_val === "") {
                    _this.activeSearch = false;
                    _this.placeholder = _this.$t('header.search');
                }
            },
            search_keydown: function () {
                const _this = this;
                if (_this.search_val !== "") {
                    _this.isSearch = true;

                } else {
                    _this.$message({
                        showClose: true,
                        message: _this.$t('notification.search_no_null_error'),
                        type: "error"
                    });
                }
            },
            closeDialog: function () {
                this.startForgingDialog = false;
                this.$store.state.mask = false;
                this.cosUpgradeDialog = false;
            },
            openAdminDialog: function (title) {
                const _this = this;
                _this.adminPasswordTitle = title;
                _this.cosUpgradeDialog = false;
                _this.adminPasswordDialog = true;
            },
            getAdminPassword: function (adminPwd) {
                const _this = this;
                _this.adminPassword = adminPwd;
                _this.adminPasswordDialog = false;
                if (_this.adminPasswordTitle === 'update') {
                    _this.updateHubVersion(adminPwd);
                }
            },
            exit: function () {
                const _this = this;
                sessionStorage.setItem("sharder.login.mode",'manual');
                window.location.href = "/";
                _this.secretPhrase = undefined;
                _this.accountRS = undefined;
                this.$global.coordinatesMap = null;
                // localStorage.setItem('peerNum', 0);
                // _this.$router.push("/login");
            },
            refreshPage: function () {
                this.$store.commit('refresh', true);
                setTimeout( () => this.$store.commit('refresh', false), 2000);
            },
            isClose: function () {
                const _this = this;
                _this.isSearch = false;
                _this.adminPasswordDialog = false;
            },
            getLatestHubVersion() {
                const _this = this;
                _this.$http.get(_this.$global.urlPrefix() + '?requestType=getLatestCosVersion').then(res => {
                    if (res.data.success) {
                        _this.latestVersion = res.data.cosver.version;
                        _this.upgradeMode = res.data.cosver.mode;
                        _this.bakMode = res.data.cosver.bakMode;
                        let bool = _this.versionCompare(_this.blockchainStatus.version, _this.latestVersion);
                        _this.isUpdate = bool;
                    } else {
                        _this.$message.error(res.data.error ? res.data.error : res.data.errorDescription);
                    }
                }).catch(err => {
                    // _this.$message.error(err.message);
                });
            },
            updateHubVersion(adminPwd) {
                const _this = this;
                let data = new FormData();
                data.append("version", _this.latestVersion);
                data.append("mode", _this.upgradeMode);
                data.append("bakMode", _this.bakMode);
                data.append("restart", "true");
                data.append("adminPassword", adminPwd);
                this.$http.post('/sharder?requestType=upgradeClient', data).then(res => {
                    if (res.data.upgraded) {
                        _this.$message.success(_this.$t('notification.update_success'));
                        _this.$store.state.mask = false;
                        _this.$router.push("/login");
                        // window.location="/";
                        _this.autoRefresh();
                    } else {
                        _this.$message.error(res.data.error ? res.data.error : res.data.errorDescription);
                    }
                }).catch(err => {
                    // _this.$message.error(err.message);
                });
            },
            versionCompare(current, latest) {
                let currentPre = parseFloat(current);
                let latestPre = parseFloat(latest);
                let currentNext = current.replace(currentPre + ".", "");
                let latestPreNext = latest.replace(latestPre + ".", "");
                if (currentPre > latestPre) {
                    return false;
                } else if (currentPre < latestPre) {
                    return true;
                } else {
                    if (currentNext >= latestPreNext) {
                        return false;
                    } else {
                        return true;
                    }
                }
            },
            openCosUpgradeDialog: function () {
                const _this = this;
                // _this.$store.state.mask = true;
                _this.cosUpgradeDialog = true;
                // get last version once
                _this.getLatestHubVersion();
            },
            autoRefresh() {
                setTimeout(() => {
                    window.location.reload();
                }, 40000);
            },
        },
        watch: {
            selectLan: function (language) {
                const _this = this;
                for (let i = 0; i < _this.language.length; i++) {
                    if (_this.language[i].value === language) {
                        _this.$i18n.locale = language;
                        _this.$store.commit('updateLang', language);
                        _this.selectLanValue = language;
                    }
                }
                _this.activeSearch = false;
                _this.placeholder = _this.$t('header.search');
            }
        },
        computed: {
            openApiProxy: function () {
                const _this = this;
                return _this.$global.isOpenApiProxy();
            }
        }
    };
</script>
<style lang="scss" type="text/scss">
    /* You can import all your SCSS variables using webpack alias*/
    /*@import '~scss_vars';*/
    @import './style.scss';
</style>
<style scoped lang="scss" type="text/scss">
@import '../../styles/css/vars.scss';
     @media only screen and (max-width: 780px) {
        .navbar_left /deep/ .el-menu--horizontal .el-menu-item:not(.is-disabled):focus, .el-menu--horizontal .el-menu-item:not(.is-disabled):hover {
            border-bottom: 2px solid $primary_color !important;
            color: $primary_color !important;
        }
    }

    .el-select-dropdown {
        .el-select-dropdown__item.selected {
            background-color: $primary_color !important;
            color: #fff !important;
        }

        .el-select-dropdown__item.selected.hover {
            background-color: $primary_color !important;
            color: #fff !important;
        }
    }

    .en_menu {
        .el-menu-item {
            font-size: 12px !important;
        }
    }

    .download_blocks_loading {
        position: fixed;
        z-index: 8888;

        right: 20px;
        top: 80px;
        width: 320px;
        min-height: 80px;
        padding: 14px;
        border-radius: 8px;
        background: #fff;
        box-shadow: 0 4px 10px #aaa;

        p {
            margin-bottom: 10px;
        }

        .download_block_progress {
            margin-top: 10px;
        }
    }

    .modal_cosUpgrade {
        position: absolute;
        background: #fff;
        top: 100px;
        width: 337px;
        border-radius: 7px;
        margin: 0 auto;
        left: 0;
        right: 0;
        z-index: 9999;
        box-shadow: 1px 1px 10px $primary_color;

        .modal-header {
            .modal-title {
                text-align: center;
                font-size: 16px;
                color: #555;
                font-weight: bold;
                line-height: 60px;
            }
        }

        .modal-body {
            padding: 20px 20px 60px !important;

            .version-info {

                span:last-child {
                    display: inline-block;
                    margin-top: 10px;
                }

                .found-new-version {
                    color: $primary_color;
                    font-weight: bold;
                }

                .no-new-version {
                    color: #ccc;
                }
            }

            .el-form {
                margin-top: 20px !important;

                .el-form-item {
                    margin-top: 15px !important;
                }

                .create_account a {
                    position: absolute;
                    right: 20px;
                    top: 0;
                    cursor: pointer;
                }
            }
            .footer-btn {
                padding: 10px 0px 10px 10px;
                float: right;
                margin: 20px 0 15px 0;

                button {
                    padding: 5px 15px 5px 15px;
                }
            }
        }
    }

    #set_admin_password_modal {
        top: 100px;
        width: 300px;
    }
</style>
