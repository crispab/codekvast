import {Injectable, Pipe, PipeTransform} from '@angular/core';
import {DatePipe} from '@angular/common';

@Pipe({name: 'ckAge'}) @Injectable()
export class CkAgePipe implements PipeTransform {

    private minuteMillis = 60 * 1000;
    private hourMillis = 60 * this.minuteMillis;
    private dayMillis = 24 * this.hourMillis;

    constructor(private datePipe: DatePipe) {
    }

    transform(value: any, pattern?: string): string {
        if (value === 0) {
            return null;
        }
        if (pattern === 'age') {
            return this.getAge(value);
        }
        return this.datePipe.transform(value, pattern);
    }

    private isDate(value: any): boolean {
        return value instanceof Date && !isNaN(value.valueOf());
    }

    private isInteger(value: any): boolean {
        return Number.isInteger(value);
    }

    private getAge(value: any): string {
        if (this.isInteger(value)) {
            return this.getAgeMillis(value);
        }
        if (this.isDate(value)) {
            return this.getAgeMillis(value.getTime());
        }
        throw new SyntaxError("CkAgePipe only understands integers and dates");
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
            age -= hours * this.hourMillis;
            result += delimiter + hours + "h";
            delimiter = " ";
        }
        if (age > this.minuteMillis) {
            let minutes = Math.trunc(age / this.minuteMillis);
            age -= minutes * this.minuteMillis;
            result += delimiter + minutes + "m";
            delimiter = " ";
        }
        return result;
    }
}
