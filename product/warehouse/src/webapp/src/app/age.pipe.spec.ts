import {DatePipe} from '@angular/common';
import {AgePipe} from './age.pipe';

let pipe: AgePipe;
let parentPipe: DatePipe;

function getPastDate(days: number, hours: number, minutes: number): Date {
    let minuteMillis = 60 * 1000;
    let hourMillis = 60 * minuteMillis;
    let dayMillis = 24 * hourMillis;
    let now = new Date().getTime();
    return new Date(now - days * dayMillis - hours * hourMillis - minutes * minuteMillis);
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

    it('Should recognize "age" pattern', () => {
        expect(pipe.transform(getPastDate(2, 4, 36), 'age')).toBe('2d 4h 36m');
    });

    it('Should throw error for non-dates and non-integers', () => {
        expect(() => pipe.transform('foobar', 'age')).toThrowError();
    });

});
