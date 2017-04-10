import {DatePipe} from '@angular/common';
import {AgePipe} from './age.pipe';

let pipe: AgePipe;
let parentPipe: DatePipe;

function getPastDate(days: number, hours: number, minutes: number, seconds: number): Date {
    let secondMillis = 1000;
    let minuteMillis = 60 * secondMillis;
    let hourMillis = 60 * minuteMillis;
    let dayMillis = 24 * hourMillis;
    let now = new Date().getTime();
    return new Date(now - days * dayMillis - hours * hourMillis - minutes * minuteMillis - seconds * secondMillis);
}

describe('AgePipe', () => {

    beforeEach(() => {
        parentPipe = {
            transform(value: any, pattern?: string): string {
                return 'parentPipe(' + pattern + ')' + value;
            }
        } as DatePipe;
        pipe = new AgePipe(parentPipe);
    });

    it('Should return null for zero', () => {
        expect(pipe.transform(0)).toBe(null);
    });

    it('Should delegate to parent when no pattern', () => {
        let value = 'foobar';
        expect(pipe.transform(value)).toBe(parentPipe.transform(value));
    });

    it('Should delegate to parent when unrecognized pattern', () => {
        let value = 'foobar';
        let pattern = 'pattern';
        expect(pipe.transform(value, pattern)).toBe(parentPipe.transform(value, pattern));
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

    it('Should throw error for non-dates and non-integers', () => {
        expect(() => pipe.transform('foobar', 'age')).toThrowError();
    });

});
