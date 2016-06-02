import {Pipe} from 'angular2/core';
import {DatePipe} from 'angular2/src/common/pipes/date_pipe';
import {InvalidPipeArgumentException} from 'angular2/src/common/pipes/invalid_pipe_argument_exception';
import {isDate, isNumber} from 'angular2/src/facade/lang';

@Pipe({name: 'ck_date'})
export class CkDatePipe extends DatePipe {

    private hourMillis = 60 * 60 * 1000;
    private dayMillis = 24 * this.hourMillis;

    transform(value: any, pattern?: string): string {
        if (value === 0) {
            return null;
        }
        if (pattern === 'age') {
            return this.getAge(value);
        }
        return super.transform(value, pattern);
    }

    supports(obj: any): boolean {
        return isDate(obj) || isNumber(obj);
    }

    private getAge(value: any): string {
        if (isNumber(value)) {
            return this.getAgeMillis(value);
        }
        if (isDate(value)) {
            return this.getAgeMillis(value.getTime())
        }
        throw new InvalidPipeArgumentException(DatePipe, value);
    }

    private getAgeMillis(value: number): string {
        let age = new Date().getTime() - value;
        let result = "";
        let delimiter = "";
        if (age > this.dayMillis) {
            let days = Math.trunc(age / this.dayMillis);
            age -= days * this.dayMillis;
            result += days + "d";
            delimiter = " ";
        }
        if (age > this.hourMillis) {
            let hours = Math.trunc(age / this.hourMillis);
            result += delimiter + hours + "h";
        }
        return result;
    }
}
