/******************************************************************************
 * Copyright © 2018 sharder.org.                                              *
 * Copyright © 2014-2018 ichaoj.com.                                          *
 *                                                                            *
 * See the LICENSE.txt file at the top-level directory of this distribution   *
 * for licensing information.                                                 *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement with ichaoj.com,   *
 * no part of the COS software, including this file, may be copied, modified, *
 * propagated, or distributed except according to the terms contained in the  *
 * LICENSE.txt file.                                                          *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

/**
 * @depends {nrs.js}
 */

var NRS = (function(NRS, $) {

    var checked = false;
    var checked2 = false;
    var isPassPhrase = false;
    var isUseNATService = false;
    var tempData = "";
    var restartNow = true;
    var version = "";
    var account = "";
    var password = "";

        $(window).load(function() {
        NRS.sendRequest("getUserConfig",{},function (response) {
            if (response["sharder.HubBindAddress"]!=null){
                $(".init_hub").hide();
                $(".returning_user").show();
                $(".create_account").show();
            }
        })
    });

    var lock = false;

    NRS.submitHubForm = function (type) {
        if (lock) {
            return;
        }
        var data = ""
        var ajaxData = "";
        if (type == 1) {
            data = NRS.getHubData(type);
            if (NRS.validation(data,type)) {
                return;
            }
            ajaxData = NRS.transferJSON(data,type);
            lock = true;
        }else {
            ajaxData = tempData;
        }
        if (type == 1) {
            $("#hub_init_loading").show();
            $.ajax({
                // url:"http://localhost:8080/bounties/hubDirectory/check/confirm.ss",
                url:"https://taskhall.sharder.org/bounties/hubDirectory/check/confirm.ss",
                type:"POST",
                data:{
                    "username":data.sharderAccount,
                    "password":data.sharderPassword,
                    "sharderAddress":data.hubBindAddress
                },
                success:function (res) {
                    ajaxData.restart = true;
                    if (res == "success") {
                        $("#p_init_hub_modal").modal("hide");
                        NRS.sendRequest("reConfig",ajaxData,function () {})
                        $("#hub_init_loading").hide();
                        $("#restart_ing").modal("show");
                    }else {
                        lock = false;
                        $("#hub_init_loading").hide();
                        $("#error_message").html($("#is_account_password_error").text());
                        $("#error_message").show();
                    }
                },
                error:function (error) {
                    lock = false;
                    $("#hub_init_loading").hide();
                    $("#error_message").html($("#is_error").text());
                    $("#error_message").show();
                }
            })
        }
        if (type == 2) {
            $("#hub_restart_loading").show();
            var adminPassword = $.trim($("#adminPassword").val());
            if (adminPassword == ""){
                $("#hub_restart_loading").hide();
                $("#error_message_restart").html($("#is_admin_password").text());
                $("#error_message_restart").show();
                return;
            }
            if (lock) {
                $("#hub_restart_loading").hide();
                return;
            }
            ajaxData.adminPassword = adminPassword;
            if (restartNow) {
                ajaxData.restart = true;
            }

            $.ajax({
                url:"/sharder?requestType=reConfig",
                data:ajaxData,
                type:"POST",
                timeout : 7000,
                dataType:"JSON",
                success:function (res) {
                    $("#hub_restart_loading").hide();
                    if (res.errorCode == 8){
                        $("#error_message_restart").html($("#is_admin_password").text());
                        $("#error_message_restart").show();
                    }
                    if (res.errorCode == 4){
                        if (res.errorDescription == "Incorrect \"adminPassword\" (the specified password does not match sharder.adminPassword)") {
                            $("#error_message_restart").html($("#is_admin_password_error").text());
                            $("#error_message_restart").show();
                        }
                        if (res.errorDescription == "Incorrect \"adminPassword\" (locked for 1 hour, too many incorrect password attempts)"){
                            $("#error_message_restart").html($("#lock_account").text());
                            $("#error_message_restart").show();
                        }
                    }
                    if (res.reconfiged) {
                        if (account != "" && password != ""){
                            $.ajax({
                                // url:"http://localhost:8080/bounties/hubDirectory/check/confirm.ss",
                                url:"https://taskhall.sharder.org/bounties/hubDirectory/check/confirm.ss",
                                type:"POST",
                                data:{
                                    "username":account,
                                    "password":password,
                                    "sharderAddress":ajaxData["sharder.HubBindAddress"]
                                },
                                success:function (res) {
                                },error:function () {
                                    $("#error_message_restart").html($("#is_error").text());
                                    $("#error_message_restart").show();
                                    return;
                                }
                            })
                        }
                        $("#restart").modal("hide");
                    }
                lock = false;
                },error:function (error,status) {
                    $("#hub_restart_loading").hide();
                    if (status == "error") {
                        if (account != "" && password != ""){
                            $.ajax({
                                // url:"http://localhost:8080/bounties/hubDirectory/check/confirm.ss",
                                url:"https://taskhall.sharder.org/bounties/hubDirectory/check/confirm.ss",
                                type:"POST",
                                data:{
                                    "username":account,
                                    "password":password,
                                    "sharderAddress":ajaxData["sharder.HubBindAddress"]
                                },
                                success:function (res) {
                                },error:function () {
                                    $("#error_message_restart").html($("#is_error").text());
                                    $("#error_message_restart").show();
                                    return;
                                }
                            })
                        }
                        $("#restart").modal("hide");
                        $("#restart_ing").modal("show");
                    }
                    lock = false;
                }
            })
        }
    }

    NRS.getHubData = function(type){
        var formData = {};
        if (type == 1) {
            var sharderPassword = $.trim($("#sharder_password").val());
            if (sharderPassword != "") {
                formData.sharderPassword = sharderPassword;
            }
            var sharderAccount = $.trim($("#sharder_account").val());
            if (sharderAccount != "") {
                formData.sharderAccount = sharderAccount;
            }
            var initAdminPassword = $.trim($("#init_admin_password").val());
            if (initAdminPassword != "") {
                formData.initAdminPassword = initAdminPassword;
            }
            var confirmAdminPassword = $.trim($("#confirm_admin_password").val());
            if (confirmAdminPassword != "") {
                formData.confirmAdminPassword = confirmAdminPassword;
            }
            var hubBindAddress = $("#bind_address").val();
            if (hubBindAddress != "") {
                formData.hubBindAddress = hubBindAddress;
            }
            var hubBindPassPhrase = $.trim($("#hubBind_pass_phrase").val());
            if (hubBindPassPhrase != "") {
                formData.hubBindPassPhrase=hubBindPassPhrase;
            }
            var hubMyAddress = $.trim($("#hub_myAddress").val());
            if (hubMyAddress != ""){
                formData.hubMyAddress = hubMyAddress;
            }
            var hubNATSServiceAddress = $.trim($("#hub_NATS_service_address").val());
            if (hubNATSServiceAddress != ""){
                formData.hubNATSServiceAddress = hubNATSServiceAddress;
            }
            var hubNATSServicePort = $.trim($("#hub_NATS_service_port").val());
            if (hubNATSServicePort != ""){
                formData.hubNATSServicePort = hubNATSServicePort;
            }
            var hubNATSClientKey = $.trim($("#hub_NATS_client_key").val());
            if (hubNATSClientKey != ""){
                formData.hubNATSClientKey = hubNATSClientKey;
            }
        }
        if (type == 2) {
            var hubNATServiceAddressConfigure = $.trim($("#hub_NAT_service_address_configure").val());
            if (hubNATServiceAddressConfigure != "") {
                formData.hubNATServiceAddressConfigure = hubNATServiceAddressConfigure;
            }
            var hubNATServicePortConfigure = $.trim($("#hub_NAT_service_port_configure").val());
            if (hubNATServicePortConfigure != "") {
                formData.hubNATServicePortConfigure = hubNATServicePortConfigure;
            }
            var hubNATClientKeyConfigure = $.trim($("#hub_NAT_client_key_configure").val());
            if (hubNATClientKeyConfigure != ""){
                formData.hubNATClientKeyConfigure = hubNATClientKeyConfigure;
            }
            var hubMyAddressConfigure = $.trim($("#hub_myAddress_configure").val());
            if (hubMyAddressConfigure != ""){
                formData.hubMyAddressConfigure = hubMyAddressConfigure;
            }
            var hubBindAddressConfigure = $.trim($("#hub_bind_address_configure").val());
            if (hubBindAddressConfigure != ""){
                formData.hubBindAddressConfigure = hubBindAddressConfigure;
            }
            var hubBindPassPhraseConfigure = $.trim($("#hub_bind_pass_phrase_configure").val());
            if (hubBindPassPhraseConfigure != ""){
                formData.hubBindPassPhraseConfigure = hubBindPassPhraseConfigure;
            }
            var newAdminPassword = $.trim($("#new_admin_password").val());
            if (newAdminPassword != ""){
                formData.newAdminPassword = newAdminPassword;
            }
            var confirmNewAdminPassword = $.trim($("#confirm_new_admin_password").val());
            if (confirmNewAdminPassword != ""){
                formData.confirmNewAdminPassword = confirmNewAdminPassword;
            }
            /*var hubConfigureAdminPassword = $.trim($("#hub_configure_admin_password").val());
            if (hubConfigureAdminPassword != ""){
                formData.hubConfigureAdminPassword = hubConfigureAdminPassword;
            }*/
        }
        return formData;
    }

    NRS.validation = function(data,type) {
        if (type == 1) {
            var errorShow = $("#error_message");
            if ($.isEmptyObject(data)) {
                errorShow.html($("#is_form_empty").text());
                errorShow.show();
                return true;
            }else{
                if (data.sharderPassword == undefined) {
                    errorShow.html($("#is_sharder_password").text());
                    errorShow.show();
                    return true;
                }
                if (data.sharderAccount == undefined) {
                    errorShow.html($("#is_sharder_account").text());
                    errorShow.show();
                    return true;
                }
                if (data.hubBindAddress == undefined) {
                    errorShow.html($("#is_bind_address").text());
                    errorShow.show();
                    return true;
                }
                if (data.hubMyAddress == undefined) {
                    errorShow.html($("#is_local_address").text());
                    errorShow.show();
                    return true;
                }
                if (data.initAdminPassword == undefined) {
                    errorShow.html($("#is_init_admin_password").text());
                    errorShow.show();
                    return true;
                }
                if (data.confirmAdminPassword == undefined) {
                    errorShow.html($("#is_confirm_admin_password").text());
                    errorShow.show();
                    return true;
                }
                if (data.initAdminPassword != data.confirmAdminPassword) {
                    errorShow.html($("#is_password_not_match").text());
                    errorShow.show();
                    return true;
                }
                if (checked2) {
                    if (data.hubBindPassPhrase == undefined) {
                        errorShow.html($("#is_bind_pass_phrase").text());
                        errorShow.show();
                        return true;
                    }
                }
                if (checked) {
                    if (data.hubNATSServiceAddress == undefined) {
                        errorShow.html($("#is_NATS_service_address").text());
                        errorShow.show();
                        return true;
                    }
                    if (data.hubNATSServicePort == undefined) {
                        errorShow.html($("#is_NATS_service_port").text());
                        errorShow.show();
                        return true;
                    }
                    if (data.hubNATSClientKey == undefined) {
                        errorShow.html($("#is_NATS_client_key").text());
                        errorShow.show();
                        return true;
                    }
                }
            }
        }
        if (type == 2) {
            var errorShow = $("#error_message_configure");
            if ($.isEmptyObject(data)) {
                errorShow.html($("#is_form_empty").text());
                errorShow.show();
                return true;
            }else{
                if (data.hubBindAddressConfigure == undefined) {
                    errorShow.html($("#is_bind_address").text());
                    errorShow.show();
                    return true;
                }
                if (data.hubMyAddressConfigure == undefined) {
                    errorShow.html($("#is_local_address").text());
                    errorShow.show();
                    return true;
                }
                /*if (data.hubNATServiceAddressConfigure == undefined) {
                    errorShow.html($("#is_NATS_service_address").text());
                    errorShow.show();
                    return true;
                }
                if (data.hubNATServicePortConfigure == undefined) {
                    errorShow.html($("#is_NATS_service_port").text());
                    errorShow.show();
                    return true;
                }
                if (data.hubNATClientKeyConfigure == undefined) {
                    errorShow.html($("#is_NATS_client_key").text());
                    errorShow.show();
                    return true;
                }*/
                /*if (data.hubConfigureAdminPassword == undefined) {
                    errorShow.html($("#is_admin_password").text());
                    errorShow.show();
                    return true;
                }*/
                /*if (checked2 && data.hubBindPassPhraseConfigure == undefined) {
                    errorShow.html($("#is_bind_pass_phrase").text());
                    errorShow.show();
                    return true;
                }*/
                if (data.newAdminPassword != undefined) {
                    if (data.confirmNewAdminPassword == undefined) {
                        errorShow.html($("#is_confirm_admin_password").text());
                        errorShow.show();
                        return true;
                    }
                    if (data.newAdminPassword != data.confirmNewAdminPassword) {
                        errorShow.html($("#is_password_not_match").text());
                        errorShow.show();
                        return true;
                    }
                }
            }
        }
        return false;
    };

    NRS.transferJSON = function(data,type){
        var val = "{";
        if (type == 1) {
            if (data.hubBindAddress != undefined) {
                val += '"sharder.HubBindAddress":' + '"'+data.hubBindAddress+'",';
            }
            if (data.hubBindPassPhrase != undefined) {
                val += '"sharder.HubBindPassPhrase":' + '"'+data.hubBindPassPhrase+'",';
            }
            if (data.hubMyAddress != undefined) {
                val += '"sharder.myAddress":' + '"'+data.hubMyAddress+'",';
            }
            if (data.hubBindAddress != undefined) {
                val += '"sharder.HubBindAddress":' + '"'+data.hubBindAddress+'",';
            }
            if (data.initAdminPassword != undefined) {
                val += '"newAdminPassword":' + '"'+data.initAdminPassword+'",';
            }
            if (data.hubNATSServiceAddress != undefined) {
                val += '"sharder.NATServiceAddress":' + '"'+data.hubNATSServiceAddress+'",';
            }
            if (data.hubNATSServicePort != undefined) {
                val += '"sharder.NATServicePort":' + '"'+data.hubNATSServicePort+'",';
            }
            if (data.hubNATSClientKey != undefined) {
                val += '"sharder.NATClientKey":' + '"'+data.hubNATSClientKey+'",';
            }
            val += '"isInit":' + true +',';
        }
        if (type == 2) {
            if (data.hubBindAddressConfigure != undefined) {
                val += '"sharder.HubBindAddress":' + '"'+data.hubBindAddressConfigure+'",';
            }
            if (data.hubMyAddressConfigure != undefined) {
                val += '"sharder.myAddress":' + '"'+data.hubMyAddressConfigure+'",';
            }
            if (data.hubBindPassPhraseConfigure != undefined) {
                val += '"sharder.HubBindPassPhrase":' + '"'+data.hubBindPassPhraseConfigure+'",';
            }
            if (data.newAdminPassword != undefined) {
                val += '"newAdminPassword":' + '"'+data.newAdminPassword+'",';
            }
            if (data.hubNATServiceAddressConfigure != undefined) {
                val += '"sharder.NATServiceAddress":' + '"'+data.hubNATServiceAddressConfigure+'",';
            }
            if (data.hubNATServicePortConfigure != undefined) {
                val += '"sharder.NATServicePort":' + '"'+data.hubNATServicePortConfigure+'",';
            }
            if (data.hubNATClientKeyConfigure != undefined){
                val += '"sharder.NATClientKey":' + '"'+data.hubNATClientKeyConfigure+'",';
            }
        }
        /*if (!checked && type == 1) {
            val = val.substring(0,val.length-1);
        }*/
        if (type == 1) {
            if (checked) {
                val += '"sharder.useNATService":' + true +',';
            }else {
                val += '"sharder.useNATService":' + false +',';
            }
            if (checked2) {
                val += '"sharder.HubBind":' + true +',';
            }else {
                val += '"sharder.HubBind":' + false +',';
            }
            if (data.initAdminPassword != undefined) {
                val += '"sharder.disableAdminPassword":' + false +',';
            }
        }else{
            if (isUseNATService && data.hubNATServiceAddressConfigure != undefined && data.hubNATServicePortConfigure != undefined && data.hubNATClientKeyConfigure != undefined) {
                val += '"sharder.useNATService":' + true +',';
            }else {
                val += '"sharder.useNATService":' + false +',';
            }
            if (isPassPhrase) {
                val += '"sharder.HubBind":' + true +',';
            }else {
                val += '"sharder.HubBind":' + false +',';
            }
            if (checked2) {
                val += '"sharder.HubBind":' + true +',';
                if (data.hubBindPassPhraseConfigure != undefined) {
                    val += '"reBind":' + true +',';
                }else {
                    val += '"reBind":' + false +',';
                }
            }else {
                val += '"sharder.HubBind":' + false +',';
                val += '"reBind":' + false +',';
            }
        }
        if (val.substring(val.length - 1, val.length) == ",") {
            val = val.substring(0,val.length-1);
        }
        val += "}";
        return $.parseJSON(val);
    }

    NRS.hubRestart = function(type){
        var adminPassword = $.trim($("#adminPassword").val());
        if (adminPassword == "" && type != 1) {
            $("#error_message_restart").html($("#is_admin_password").text());
            $("#error_message_restart").show();
            return;
        }
        $("#error_message_restart").hide();
        if (type == 1) {
            $.ajax({
                url:"/sharder?requestType=restart",
                type:"POST"
            });
        }
    }

    NRS.isRestart = function(){
         account = $.trim($("#sharder_account_configure").val());
         password = $.trim($("#sharder_password_configure").val());
         var temp = NRS.getHubData(2);
        if (NRS.validation(temp,2)) {
            return;
        }
         tempData = NRS.transferJSON(temp,2);
        $("#restart").modal({backdrop:'static',keyboard:false});
    }

    NRS.hubConfigure = function(){
        NRS.sendRequest("getState",{},function (response) {
            if (response.version != "" && response.version != null) {
                $("#version").html(response.version);
                NRS.sendRequest("getLastestHubVersion",{},function (result) {
                    if (result.version != null && result.version != response.version) {
                        version = result.version;
                        $("#update").show();
                    }
                })
            }
        })
        NRS.sendRequest("getUserConfig",{},function (response) {
            if (response != null){
                if (response["sharder.useNATService"] == "true"){
                    isUseNATService = true;
                    $("#is_NAT_configure").prop("checked",true);
                    $("#NAT_hub_configure").show();
                    $("#hub_NAT_service_address_configure").val(response["sharder.NATServiceAddress"]);
                    $("#hub_NAT_service_port_configure").val(response["sharder.NATServicePort"]);
                    $("#hub_NAT_client_key_configure").val(response["sharder.NATClientKey"]);
                }
                if (response["sharder.HubBind"] == "true") {
                    isPassPhrase = true;
                    checked2 = true;
                    $("#is_forging_configure").prop("checked",true);
                    $("#passPhrase_configure").show();
                    $("#enable").show();
                }
                $("#hub_myAddress_configure").val(response["sharder.myAddress"]);
                $("#hub_myAddress_configure").prop("disabled",true);
                $("#hub_bind_address_configure").val(response["sharder.HubBindAddress"]);
            }
        })
        $("#p_configure_hub_modal").modal("show");
    }

  /*  NRS.hubUpdate = function(){
        $("#isUpdate").modal("show");
    }*/

    NRS.hubUpdateNow = function () {
        var adminPassword = $.trim($("#adminPassword_update").val());
        if (adminPassword == ""){
            $("#error_message_update").html($("#is_admin_password").text());
            $("#error_message_update").show();
            return;
        }
        NRS.sendRequest("upgradeClient",{"version":version,"adminPassword":adminPassword,"restart":true},function (response) {
            if ((response.errorCode != null || response.errorCode != undefined) && response.errorCode != -1) {
                $("#error_message_update").html(response.errorDescription);
                $("#error_message_update").show();
                return;
            } else {
                $("#restart_ing").modal("show");
            }
            /*if (response.errorCode == 4) {
                $("#error_message_update").html($("#is_admin_password_error").text());
                $("#error_message_update").show();
                return;
            }
            if (response.errorCode == -1) {
                $("#error_message_update").html(response.errorDescription);
                $("#error_message_update").show();
                return;
            }
            if (response.upgraded){
                $("#isUpdate").modal("hide");
                $("#update").hide();
            }*/
        })
    }

    var open = true;

    NRS.isShowNatConfig = function(){
        var configChecked = $("#is_NAT_configure").prop("checked");
        if (configChecked) {
            $("#NAT_hub_configure").show();
            $("#hub_myAddress_configure").prop("disabled",true);
            NRS.isNATConfigure();
        }else {
            $("#NAT_hub_configure").hide();
            $("#hub_myAddress_configure").prop("disabled",false);
        }
    }

    NRS.isNATConfigure = function(){
        if ($("#is_NAT_configure").prop("checked")) {
            var account = $.trim($("#sharder_account_configure").val());
            var password = $.trim($("#sharder_password_configure").val());
            if (account != "" && password != ""){

                $("#error_message_configure").hide();
                /*if (open) {*/
                $("#hub_configure_loading").show();
                $.ajax({
                    // url:"http://localhost:8080/bounties/hubDirectory/check.ss",
                    url:"https://taskhall.sharder.org/bounties/hubDirectory/check.ss",
                    type:"POST",
                    dataType:"JSON",
                    data:{
                        "username":account,
                        "password":password
                    },
                    success:function (res) {
                        if (res.code == 404 && res.errorType == "unifiedUserIsNull") {
                            $("#hub_configure_loading").hide();
                            $("#error_message_configure").html($("#is_account_password_error").text());
                            $("#error_message_configure").show();
                            return;
                        }
                        if (res.code == 404 && res.errorType == "hubDirectoryIsNull") {
                            $("#hub_configure_loading").hide();
                            $("#error_message_configure").html($("#no_hub_configure").text());
                            $("#error_message_configure").show();
                            return;
                        }
                        if (res.code == 200){
                            open = false;
                            checked = $("#is_NAT_configure").prop("checked");
                            if (res.data.hubAddress != null) {
                                $("#hub_myAddress_configure").prop("disabled",true);
                                $("#hub_myAddress_configure").val(res.data.hubAddress);
                            }
                            if (res.data.natServiceAddress != null) {
                                $("#hub_NAT_service_address_configure").val(res.data.natServiceAddress);
                            }
                            if (res.data.natServicePort != null) {
                                $("#hub_NAT_service_port_configure").val(res.data.natServicePort);
                            }
                            if (res.data.natClientKey != null) {
                                $("#hub_NAT_client_key_configure").val(res.data.natClientKey);
                            }
                            isUseNATService = true;
                            $("#hub_configure_loading").hide();
                            $("#is_NAT_configure").prop("checked", true);
                            $("#NAT_hub_configure").show();
                        }
                    },
                    error:function (e) {
                        $("#hub_configure_loading").hide();
                        $("#error_message_configure").html($("#is_account_password_error").text());
                        $("#error_message_configure").show();
                    }
                })
            }
        }
    }

    NRS.isNat = function(){
        checked = $("#is_NATS").prop("checked");
        if (checked) {
            if ($("#hub_NATS_client_key").val() == ""){
                if ($.trim($("#sharder_account").val()) != "" && $.trim($("#sharder_password").val()) != "") {
                    NRS.isNATS();
                }
            }
            $("#NATS_configure").show();
            $("#hub_myAddress").prop("disabled",true);
            
        }else {
            $("#hub_myAddress").prop("disabled",false);
            $("#NATS_configure").hide();
        }
    }

    NRS.isNATS = function () {
        var account = $.trim($("#sharder_account").val());
        var password = $.trim($("#sharder_password").val());
        if (account == "" || password == ""){
            $("#error_message").html($("#is_account_password").text());
            $("#error_message").show();
            return;
        }
        $("#error_message").hide();
        if (checked) {
                $("#hub_init_loading").show();
                $.ajax({
                    // url:"http://localhost:8080/bounties/hubDirectory/check.ss",
                    url:"https://taskhall.sharder.org/bounties/hubDirectory/check.ss",
                    type:"POST",
                    dataType:"JSON",
                    data:{
                        "username":account,
                        "password":password
                    },
                    success:function (res) {
                        if (res.code == 404 && res.errorType == "unifiedUserIsNull") {
                            $("#hub_init_loading").hide();
                            $("#error_message").html($("#is_account_password_error").text());
                            $("#error_message").show();
                            return;
                        }
                        if (res.code == 404 && res.errorType == "hubDirectoryIsNull") {
                            $("#hub_init_loading").hide();
                            $("#error_message").html($("#no_hub_configure").text());
                            $("#error_message").show();
                            return;
                        }
                        if (res.code == 200){
                            open = false;
                            if (res.data.hubAddress != null) {
                                $("#hub_myAddress").prop("disabled",true);
                                $("#hub_myAddress").val(res.data.hubAddress);
                            }
                            if (res.data.natServiceAddress != null) {
                                $("#hub_NATS_service_address").val(res.data.natServiceAddress);
                            }
                            if (res.data.natServicePort != null) {
                                $("#hub_NATS_service_port").val(res.data.natServicePort);
                            }
                            if (res.data.natClientKey != null) {
                                $("#hub_NATS_client_key").val(res.data.natClientKey);
                            }
                        }
                        $("#hub_init_loading").hide();
                    },
                    error:function (e) {
                        $("#hub_init_loading").hide();
                        $("#error_message").html($("#is_error").text());
                        $("#error_message").show();
                        $("#is_NATS").prop("checked",false);
                        return;
                    }
                })
        }
    }

    NRS.restartLater = function(){
        restartNow = false;
        NRS.submitHubForm(2);
    }
    NRS.restartNow = function(){
        restartNow = true;
        NRS.submitHubForm(2);
    }

    NRS.isForging = function (type) {
        if (type == 1) {
            checked2 = $("#is_forging").prop("checked");
            if (checked2) {
                $("#passPhrase").show();
            }else{
                $("#passPhrase").hide();
            }
        }else {
            checked2 = $("#is_forging_configure").prop("checked");
            if (checked2) {
                $("#passPhrase_configure").show();
            }else{
                $("#passPhrase_configure").hide();
            }
        }
    }

    NRS.openInitModal = function(){
        /*$("#NATS_configure").hide();*/
        $("#is_NATS").prop("checked",true);
        $("#is_forging").prop("checked",true);
        $("#NATS_configure").show();
        $("#passPhrase").show();
        checked = true;
        checked2 = true;
        $("#p_init_hub_modal").modal({backdrop: 'static', keyboard: false});
    }

    $("#reboot_confirm_btn").click(function(e) {
        e.preventDefault();
        NRS.sendRequest("restart",{"adminPassword":$("#adminPassword4reboot").val()},function (response) {
            if ((response.errorCode != null || response.errorCode != undefined) && response.errorCode != -1) {
                $("#error_message_reboot").html(response.errorDescription);
                $("#error_message_reboot").show();
                return;
            } else {
                $("#restart_ing").modal("show");
            }
        })
    });

    $("#reset_confirm_btn").click(function(e) {
        e.preventDefault();
        NRS.sendRequest("recovery",{"adminPassword":$("#adminPassword4reset").val(),"restart":true},function (response) {
            if ((response.errorCode != null || response.errorCode != undefined) && response.errorCode != -1) {
                $("#error_message_reset").html(response.errorDescription);
                $("#error_message_reset").show();
                return;
            } else {
                if (NRS.database) {
                    //noinspection JSUnresolvedFunction
                    indexedDB.deleteDatabase(NRS.database.name);
                }
                if (NRS.legacyDatabase) {
                    //noinspection JSUnresolvedFunction
                    indexedDB.deleteDatabase(NRS.legacyDatabase.name);
                }
                NRS.removeItem("logged_in");
                NRS.removeItem("savedNxtAccounts");
                NRS.removeItem("language");
                NRS.removeItem("savedPassphrase");
                NRS.localStorageDrop("data");
                NRS.localStorageDrop("polls");
                NRS.localStorageDrop("contacts");
                NRS.localStorageDrop("assets");
                $("#restart_ing").modal("show");
            }
        })
    });

    return NRS;
}(NRS || {}, jQuery));

