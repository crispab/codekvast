import {DatePipe} from '@angular/common';
import {AgePipe} from './age.pipe';

let pipe: AgePipe;
let parentPipe: DatePipe;

function getPastDate(days: number, hours: number, minutes: number, seconds: number): Date {
    const secondMillis = 1000;
    const minuteMillis = 60 * secondMillis;
    const hourMillis = 60 * minuteMillis;
    const dayMillis = 24 * hourMillis;
    const now = new Date().getTime();
    return new Date(now - days * dayMillis - hours * hourMillis - minutes * minuteMillis - seconds * secondMillis);
}

function getFutureDate(days: number, hours: number, minutes: number, seconds: number): Date {
    const secondMillis = 1000;
    const minuteMillis = 60 * secondMillis;
    const hourMillis = 60 * minuteMillis;
    const dayMillis = 24 * hourMillis;
    const now = new Date().getTime();
    return new Date(now + days * dayMillis + hours * hourMillis + minutes * minuteMillis + seconds * secondMillis);
}

describe('AgePipe', () => {

    beforeEach(() => {
        parentPipe = {
            transform(value: any, pattern?: string): string {
                return `parentPipe(${pattern})${value}`;
            }
        } as DatePipe;
        pipe = new AgePipe(parentPipe);
    });

    it('Should return null for undefined', () => {
        expect(pipe.transform(undefined)).toBe(null);
    });

    it('Should return null for null', () => {
        expect(pipe.transform(null)).toBe(null);
    });

    it('Should return null for zero', () => {
        expect(pipe.transform(0)).toBe(null);
    });

    it('Should delegate to parent when no pattern', () => {
        const value = 'foobar';
        expect(pipe.transform(value)).toBe(parentPipe.transform(value));
    });

    it('Should delegate to parent when unrecognized pattern', () => {
        const value = 'foobar';
        const pattern = 'pattern';
        expect(pipe.transform(value, pattern)).toBe(parentPipe.transform(value, pattern));
    });

    it('Should transform getPastDate(7, 4, 36, 12) to "7d"', () => {
        expect(pipe.transform(getPastDate(7, 4, 36, 12), 'age')).toBe('7d');
    });

    it('Should transform getPastDate(2, 4, 36, 12) to "2d 4h"', () => {
        expect(pipe.transform(getPastDate(2, 4, 36, 12), 'age')).toBe('2d 4h');
    });

    it('Should transform getPastDate(0, 4, 36, 12) to "4h 36m"', () => {
        expect(pipe.transform(getPastDate(0, 4, 36, 12), 'age')).toBe('4h 36m');
    });

    it('Should transform getPastDate(0, 0, 36, 12) to "36m 12s"', () => {
        expect(pipe.transform(getPastDate(0, 0, 36, 12), 'age')).toBe('36m 12s');
    });

    it('Should transform getFutureDate(0, 0, 36, 12) to "in 36m 12s"', () => {
        expect(pipe.transform(getFutureDate(0, 0, 36, 12), 'age')).toMatch(/in 36m (11s|12s)/);
    });

    it('Should throw error for non-dates and non-integers', () => {
        expect(() => pipe.transform('foobar', 'age')).toThrowError();
    });

});
