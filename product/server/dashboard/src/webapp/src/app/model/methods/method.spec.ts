import {Method} from './method';

describe('Method', () => {

  it('should handle undefined annotations', () => {
    let m = new Method()
    m.methodAnnotation = undefined
    m.methodLocationAnnotation = undefined
    m.typeAnnotation = undefined
    m.packageAnnotation = undefined
    expect(Method.hasAnnotation(m)).toBeFalse();
  });

  it('should handle methodAnnotation', () => {
    let m = new Method()
    m.methodAnnotation = 'methodAnnotation'
    expect(Method.hasAnnotation(m)).toBeTrue();
  });

  it('should handle methodLocationAnnotation', () => {
    let m = new Method()
    m.methodLocationAnnotation = 'methodLocationAnnotation'
    expect(Method.hasAnnotation(m)).toBeTrue();
  });

  it('should handle typeAnnotation', () => {
    let m = new Method()
    m.typeAnnotation = 'typeAnnotation'
    expect(Method.hasAnnotation(m)).toBeTrue();
  });

  it('should handle packageAnnotation', () => {
    let m = new Method()
    m.packageAnnotation = 'packageAnnotation'
    expect(Method.hasAnnotation(m)).toBeTrue();
  });

  it('should handle all annotations', () => {
    let m = new Method()
    m.methodAnnotation = 'methodAnnotation'
    m.methodLocationAnnotation = 'methodLocationAnnotation'
    m.typeAnnotation = 'typeAnnotation'
    m.packageAnnotation = 'packageAnnotation'
    expect(Method.hasAnnotation(m)).toBeTrue();
  });

  it('should handle blank annotations', () => {
    let m = new Method()
    m.methodAnnotation = ' '
    m.methodLocationAnnotation = ' '
    m.typeAnnotation = ' '
    m.packageAnnotation = ' '
    expect(Method.hasAnnotation(m)).toBeFalse();
  });

  it('should handle null annotations', () => {
    let m = new Method()
    m.methodAnnotation = null
    m.methodLocationAnnotation = null
    m.typeAnnotation = null
    m.packageAnnotation = null
    expect(Method.hasAnnotation(m)).toBeFalse();
  });

  it('should strip arguments from signature', () => {
    let m = new Method()
    m.signature = 'someSignature(String p1)'
    expect(Method.stripArgumentsFromSignature(m)).toBe('someSignature')
  })

  it('should strip arguments from signature without args', () => {
    let m = new Method()
    m.signature = 'someSignature'
    expect(Method.stripArgumentsFromSignature(m)).toBe('someSignature')
  })

});
