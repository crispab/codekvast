import {InvocationStatusPipe} from './invocation-status.pipe';

const pipe = new InvocationStatusPipe();

describe('InvocationStatusPipe', () => {

    it('Should transform("EXCLUDED_SINCE_TRIVIAL") to "Excluded since trivial"', () => {
        expect(pipe.transform('EXCLUDED_SINCE_TRIVIAL')).toBe('Excluded since trivial');
    });

    it('Should transform(["NOT_INVOKED", "EXCLUDED_SINCE_TRIVIAL"]) to "Excluded since trivial, Not invoked"', () => {
        expect(pipe.transform(['NOT_INVOKED', 'EXCLUDED_SINCE_TRIVIAL'])).toBe('Excluded since trivial, Not invoked');
    });

    it('Should transform(0) to "0"', () => {
        expect(pipe.transform(0)).toBe('0');
    });

    it('Should transform(null) to null', () => {
        expect(pipe.transform(null)).toBeNull();
    });
});
