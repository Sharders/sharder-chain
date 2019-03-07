import common from "./common";

const httpScheme = 'http://';
const httpsScheme = 'https://';

export const FoundationApiUrls = {
    natRegister: {
        eoLinkerUrl: 'http://result.eolinker.com/iDmJAldf2e4eb89669d9b305f7e014c215346e225f6fe41?uri=http://localhost:8080/bounties/hubDirectory/register.ss',
        local: 'http://localhost:8080/bounties/hubDirectory/register.ss',
        path: '/bounties/hubDirectory/register.ss'
    },
    hubSettingConfirm: {
        eoLinkerUrl: 'http://result.eolinker.com/iDmJAldf2e4eb89669d9b305f7e014c215346e225f6fe41?uri=http://sharder.org/bounties/hubDirectory/check/confirm.ss',
        local: 'http://localhost:8080/bounties/hubDirectory/check/confirm.ss',
        path: '/bounties/hubDirectory/check/confirm.ss'
    },
    hubSettingAccountCheck: {
        eoLinkerUrl: 'http://result.eolinker.com/iDmJAldf2e4eb89669d9b305f7e014c215346e225f6fe41?uri=http://sharder.org/bounties/hubDirectory/check.ss',
        local: 'http://localhost:8080/bounties/hubDirectory/check.ss',
        path: '/bounties/hubDirectory/check.ss',
    },
};

/**
 * dynamically fetch Foundation API URLs
 * @param apiType
 * @returns {*}
 */
export function getCommonFoundationApiUrl(apiType) {
    if (common.isMainNet() || common.isTestNet()) {
        return httpScheme + common.getSharderFoundationHost() + apiType.path;
    }
    return common.useEoLinker() ? apiType.eoLinkerUrl : apiType.local;
}
