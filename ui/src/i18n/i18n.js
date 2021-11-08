import Vue from 'vue'
import VueI18n from 'vue-i18n'
import messages from './langs'
import locale from 'element-ui/lib/locale';
import store from '../store/index';
// import Util from '../utils/common';

/*Vue.use(VueI18n);*/
Vue.use(VueI18n, {
    i18n: function(path, options) {
        let value = i18n.t(path, options);
        if (value !== null && value !== undefined) {
            return value
        }
        return ''
    }
});

//从localStorage中拿到用户的语言选择，如果没有，那默认中文。
const i18n = new VueI18n({
    // locale: Util.getLocalStorage("lang") || 'cn',
    locale: store.state.currentLang || 'cn',
    messages,
});
locale.i18n((key, value) => i18n.t(key, value)); //为了实现element插件的多语言切换
export default i18n
