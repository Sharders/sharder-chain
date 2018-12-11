import enLocale from 'element-ui/lib/locale/lang/en'

const en = {
    login: {
        'login_tip1':"Log in with your TestNet account - not your real one!",
        'login_tip2':"Warning: You are connected to the TestNet. Do not use your real passphrase! ",
        'language':'language',
        'login':'login',
        'secret_login':"Passphrase",
        'account_login':"account",
        'login_placeholder':"请输入账户密钥en",
        'sharder_account':"Sharder account",
        'register_tip':"don't have an account? click here to create one!",
        'welcome_tip':"Welcome to Sharder",
        'init_hub':"init HUB",
    },
    hubsetting: {
        'enable_nat_traversal':"",
        'sharder_account':"",
        'sharder_account_password':"",
        'nat_traversal_address':"",
        'nat_traversal_port':"",
        'nat_traversal_clent_privateKey':"",
        'public_ip_address':"",
        'token_address':"",
        'enable_auto_mining':"",
        'set_mnemonic_phrase':"",
        'set_password':"",
        'confirm_password':"",
        'confirm_restart':"确认后重启en",
    },
    ...enLocale
};

export default en;
