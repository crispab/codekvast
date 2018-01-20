import {Injectable, Pipe, PipeTransform} from '@angular/core';
import {DatePipe} from '@angular/common';

@Pipe({name: 'ckAge'}) @Injectable()
export class AgePipe implements PipeTransform {

    private secondMillis = 1000;
    private minuteMillis = 60 * this.secondMillis;
    private hourMillis = 60 * this.minuteMillis;
    private dayMillis = 24 * this.hourMillis;

    constructor(private datePipe: DatePipe) {
    }

    transform(value: any, pattern?: string): string {
        if (value === undefined || value == null || value === 0) {
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
            return this.prettyPrintAgeMillis(value);
        }
        if (this.isDate(value)) {
            return this.prettyPrintAgeMillis(value.getTime());
        }
        throw new SyntaxError('AgePipe only understands integers and dates');
    }

    private prettyPrintAgeMillis(value: number): string {
        // TODO: respect LOCALE_ID

        let age = new Date().getTime() - value;
        let result = '';
        if (age < 0) {
            result = 'in ';
            age = -age;
        }

        let delimiter = '';
        let fields = 0;
        if (age > this.dayMillis) {
            let days = Math.trunc(age / this.dayMillis);
            age -= days * this.dayMillis;
            result += days + 'd';
            delimiter = ' ';
            fields += 1;
        }
        if (fields < 2 && age > this.hourMillis) {
            let hours = Math.trunc(age / this.hourMillis);
            result += delimiter + hours + 'h';
            age -= hours * this.hourMillis;
            delimiter = ' ';
            fields += 1;
        }
        if (fields < 2 && age > this.minuteMillis) {
            let minutes = Math.trunc(age / this.minuteMillis);
            result += delimiter + minutes + 'm';
            age -= minutes * this.minuteMillis;
            delimiter = ' ';
            fields += 1;
        }
        if (fields < 2 && age > this.secondMillis) {
            let seconds = Math.trunc(age / this.secondMillis);
            result += delimiter + seconds + 's';
            // age -= seconds * this.secondMillis;
            // delimiter = ' ';
            // fields += 1;
        }
        return result;
    }
}
