import zhLocale from 'element-ui/lib/locale/lang/zh-CN'

const cn = {

    login: {
        'login_tip1':"请用您的测试网络账户登录-不是您的真实账户!",
        'login_tip2':"警告:您已连接到测试网络,不要使用您的真实密钥!",
        'language':"语言",
        'login':"登录",
        'secret_login':"密钥登录",
        'account_login':"账户登录",
        'login_placeholder':"请输入账户密钥",
        'sharder_account':"Sharder账户",
        'register_tip':"没有账户? 创建账户",
        'welcome_tip':"欢迎来到豆匣链",
        'init_hub':"初始化HUB",
    },
    hubsetting: {
        'enable_nat_traversal':"启动内网穿透服务:",
        'sharder_account':"Sharder官网账户:",
        'sharder_account_password':"Sharder官网密码:",
        'nat_traversal_address':"穿透服务地址:",
        'nat_traversal_port':"穿透服务端口:",
        'nat_traversal_clent_privateKey':"穿透服务客户端秘钥:",
        'public_ip_address':"公网地址:",
        'token_address':"关联SS地址:",
        'enable_auto_mining':"是否开启挖矿:",
        'set_mnemonic_phrase':"绑定助记词:",
        'set_password':"初始化管理员密码:",
        'confirm_password':"确认管理员密码:",
        'confirm_restart':"确认后重启",
    },
    ...zhLocale
};

export default cn;
