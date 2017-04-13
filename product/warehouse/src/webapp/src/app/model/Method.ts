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
    statuses: string[];
    tags: string[];
    trackedPercent: number;
    visibility: string;
}
