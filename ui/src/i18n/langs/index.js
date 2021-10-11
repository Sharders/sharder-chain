import en from './en';
import enSharder from './sharder/en';
import cn from './cn';
import cnSharder from './sharder/cn';
import global from "../../utils/common";
export default {
    en: global.projectName === 'mw' ? en : enSharder,
    cn: global.projectName === 'mw' ? cn : cnSharder,
}
