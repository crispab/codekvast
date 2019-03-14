import {Environment} from './Environment';
import {Application} from './Application';

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

    static hasInconsistentTracking(m: Method) {
        return m.trackedPercent > 0 && m.trackedPercent < 100;
    }
}
