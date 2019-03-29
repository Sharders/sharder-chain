import common from "./common";

const httpScheme = 'http://';
const httpsScheme = 'https://';

export const FoundationApiUrls = {
    natRegister: {
        eoLinkerUrl: '',
        local: 'http://localhost:8080/sc/natServices/register',
        path: '/sc/natServices/register'
    },
    hubSettingConfirm: {
        eoLinkerUrl: '',
        local: 'http://localhost:8080/sc/natServices/bind',
        path: '/sc/natServices/bind'
    },
    fetchNatServiceConfig: {
        eoLinkerUrl: '',
        local: 'http://localhost:8080/sc/natServices/fetch',
        path: '/sc/natServices/fetch',
    },
};

/**
 * dynamically fetch Foundation API URLs
 * @param apiType
 * @returns {*}
 */
export function getCommonFoundationApiUrl(apiType) {
    if (common.isMainNet() || common.isTestNet() || common.isDevNet()) {
        return httpsScheme + common.getSharderFoundationHost() + apiType.path;
    }
    return common.useEoLinker() ? apiType.eoLinkerUrl : apiType.local;
}
