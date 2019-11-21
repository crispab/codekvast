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

    static hasInconsistentTracking(m: Method) {
        return m.trackedPercent > 0 && m.trackedPercent < 100;
    }

    static stripArgumentsFromSignature(m: Method) {
        let lparen = m.signature.indexOf('(');
        return m.signature.slice(0, lparen < 0 ? m.signature.length : lparen);
    }
}
