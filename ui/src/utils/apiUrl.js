import common from "./common";

const httpScheme = 'http://';
const httpsScheme = 'https://';

export const FoundationApiUrls = {
    natRegister: {
        eoLinkerUrl: '',
        local: 'http://localhost:8080/sc/natServices/register',
        path: '/sc/natServices/register'
    },
    fetchNatServiceConfig: {
        eoLinkerUrl: '',
        local: 'http://localhost:8080/sc/natServices/fetch',
        path: '/sc/natServices/fetch',
    },
    natReservedBinding:{
        eoLinkerUrl: '',
        local: 'http://localhost:8080/sc/natServices/reservedBinding',
        path: '/sc/natServices/reservedBinding',
    },
};

/**
 * dynamically fetch Foundation API URLs
 * @param apiType
 * @returns {*}
 */
export function getCommonFoundationApiUrl(apiType) {
    if (!common.useLocal()) {
        return httpsScheme + common.getSharderFoundationHost() + apiType.path;
    }
    return common.useEoLinker() ? apiType.eoLinkerUrl : apiType.local;
}
