export class Environment {
    collectedDays: number;
    collectedSinceMillis: number;
    collectedToMillis: number;
    hostNames: String[];
    invokedAtMillis: number;
    name: String;
    tags: String[];

    // computed fields
    collectedSince: Date;
    collectedTo: Date;
    invokedAt: Date;
}
