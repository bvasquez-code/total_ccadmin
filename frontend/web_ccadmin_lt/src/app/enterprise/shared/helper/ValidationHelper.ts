export class ValidationHelper {


    static validLengthString(val: string, length: number, messageError: string) {
        if (val.length > length) {
            throw new Error(messageError);
        }
    }

    static validNumber(val: any, maxVal: number | any, minVal: number | any, messageError: string) {
        if (isNaN(val)) {
            throw new Error(messageError);
        }
        if (maxVal != null) {
            if (Number(val) > maxVal) {
                throw new Error(messageError + ` maximo valor permitido : ${maxVal}`);
            }
        }
        if (minVal != null) {
            if (Number(val) < minVal) {
                throw new Error(messageError + ` minimo valor permitido : ${minVal}`);
            }
        }
    }

    staticcompareDates(dateMax: Date, dateMin: Date, messageError: string) {
        if (dateMax > dateMin) {
            throw new Error(messageError);
        }
    }

    static isValidString(input: string, messageError: string, regex: RegExp = /^[a-zA-Z0-9]*$/) {
        if (!regex.test(input)) {
            throw new Error(messageError);
        }
    }

    static validateIsNotEmpty(val: any, messageError: string) {
        if (val == null || val == undefined || String(val).trim() == "") {
            throw new Error(messageError);
        }
    }

    static validateInList(val: any, list: any[], messageError: string) {
        if (val == null || val == undefined) throw new Error(messageError);
        if (!list.includes(String(val).trim().toUpperCase())) {
            throw new Error(messageError);
        }
    }

}
