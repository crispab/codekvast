import {Environment} from './environment';
import {Application} from './application';

export class Method {
    collectedDays: number;
    collectedInEnvironments: Environment[];
    collectedSinceMillis: number;
    collectedToMillis: number;
    declaringType: string;
    id: number;
    lastInvokedAtMillis: number;
    modifiers: string;
    occursInApplications: Application[];
    packageName: string;
    signature: string;
    bridge: boolean;
    synthetic: boolean;
    statuses: string[];
    tags: string[];
    trackedPercent: number;
    visibility: string;
    locations: string[];
    methodAnnotation: string;
    methodLocationAnnotation: string;
    typeAnnotation: string;
    packageAnnotation: string;

    static hasInconsistentTracking(m: Method) {
        return m.trackedPercent > 0 && m.trackedPercent < 100;
    }

    static stripArgumentsFromSignature(m: Method) {
        const lparen = m.signature.indexOf('(');
        return m.signature.slice(0, lparen < 0 ? m.signature.length : lparen);
    }

    static isProbablyGone(m: Method, days: number) {
        const lastReportAgeInMillis = new Date().getTime() - m.collectedToMillis;
        const lastReportAgeInDays = lastReportAgeInMillis / 1000 / 60 / 60 / 24;
        return lastReportAgeInDays >= days;
    }

    static hasAnnotation(m: Method) {
        return !this.isNullOrEmpty(m.methodAnnotation)
            || !this.isNullOrEmpty(m.methodLocationAnnotation)
            || !this.isNullOrEmpty(m.typeAnnotation)
            || !this.isNullOrEmpty(m.packageAnnotation);
    }

    private static isNullOrEmpty(s: string) {
        return !s || s.trim().length === 0;
    }

}
