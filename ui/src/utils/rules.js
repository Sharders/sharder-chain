/**
 * must required field
 * @param message tips
 * @returns {{validator: validator, trigger: string, required: boolean}}
 */
function required(message) {
    return {
        required: true,
        validator: (rule, value, callback) => {
            // n个空格
            const reg = /^\s*$/;
            if (value) {
                if (reg.test(value)) {
                    callback(new Error(message ? message : 'Required'));
                } else {
                    callback();
                }
            } else {
                callback(new Error(message ? message : 'Required'));
            }

        },
        trigger: 'blur'
    }
}

/**
 * 非负整数 non-negative integer
 * @param message Tips
 * @returns {{validator: validator, trigger: string}}
 */
function nonNegativeInteger(message) {
    return {
        validator: (rule, value, callback) => {
            const reg = /^[0-9]+$/;
            if (!value) {
                callback();
            }
            if (!reg.test(value)) {
                callback(new Error(message ? message : 'Must Be Non-negative Integer'));
            } else {
                callback();
            }
        },
        trigger: 'blur'
    }
}

/**
 * 整数 integer
 * @param message tips
 * @returns {{validator: validator, trigger: string}}
 */
function integer(message) {
    return {
        validator: (rule, value, callback) => {
            const reg = /^(-)?[0-9]+$/;
            if (!value) {
                callback();
            }
            if (!reg.test(value)) {
                callback(new Error(message ? message : 'Must Be Integer'));
            } else {
                callback();
            }
        },
        trigger: 'blur'
    }
}

/**
 * 校验SS地址 validate SS address
 * @param message tips
 * @returns {{validator: validator, required: boolean}}
 */
function ssAddress(message) {
    return {
        required: true,
        validator: (rule, value, callback) => {
            const reg = /SSA-([A-Z0-9]{4}-){3}[A-Z0-9]{5}/;
            if (value) {
                if (reg.test(value)) {
                    callback();
                } else {
                    callback(new Error(message ? message : 'SS Address Format Error'));
                }
            } else {
                callback(new Error('Required'));
            }
        }
    }
}

export default {
    required,
    nonNegativeInteger,
    integer,
    ssAddress,
}
