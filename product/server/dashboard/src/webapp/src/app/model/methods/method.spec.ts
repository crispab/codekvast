import {Method} from './method';

describe('Method', () => {

  it('should handle undefined annotations', () => {
    const m = new Method();
    m.methodAnnotation = undefined;
    m.methodLocationAnnotation = undefined;
    m.typeAnnotation = undefined;
    m.packageAnnotation = undefined;
    expect(Method.hasAnnotation(m)).toBeFalse();
  });

  it('should handle methodAnnotation', () => {
    const m = new Method();
    m.methodAnnotation = 'methodAnnotation';
    expect(Method.hasAnnotation(m)).toBeTrue();
  });

  it('should handle methodLocationAnnotation', () => {
    const m = new Method();
    m.methodLocationAnnotation = 'methodLocationAnnotation';
    expect(Method.hasAnnotation(m)).toBeTrue();
  });

  it('should handle typeAnnotation', () => {
    const m = new Method();
    m.typeAnnotation = 'typeAnnotation';
    expect(Method.hasAnnotation(m)).toBeTrue();
  });

  it('should handle packageAnnotation', () => {
    const m = new Method();
    m.packageAnnotation = 'packageAnnotation';
    expect(Method.hasAnnotation(m)).toBeTrue();
  });

  it('should handle all annotations', () => {
    const m = new Method();
    m.methodAnnotation = 'methodAnnotation';
    m.methodLocationAnnotation = 'methodLocationAnnotation';
    m.typeAnnotation = 'typeAnnotation';
    m.packageAnnotation = 'packageAnnotation';
    expect(Method.hasAnnotation(m)).toBeTrue();
  });

  it('should handle blank annotations', () => {
    const m = new Method();
    m.methodAnnotation = ' ';
    m.methodLocationAnnotation = ' ';
    m.typeAnnotation = ' ';
    m.packageAnnotation = ' ';
    expect(Method.hasAnnotation(m)).toBeFalse();
  });

  it('should handle null annotations', () => {
    const m = new Method();
    m.methodAnnotation = null;
    m.methodLocationAnnotation = null;
    m.typeAnnotation = null;
    m.packageAnnotation = null;
    expect(Method.hasAnnotation(m)).toBeFalse();
  });

  it('should strip arguments from signature', () => {
    const m = new Method();
    m.signature = 'someSignature(String p1)';
    expect(Method.stripArgumentsFromSignature(m)).toBe('someSignature');
  });

  it('should strip arguments from signature without args', () => {
    const m = new Method();
    m.signature = 'someSignature';
    expect(Method.stripArgumentsFromSignature(m)).toBe('someSignature');
  });

});
