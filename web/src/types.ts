export type Task={url:string;Task:string;Notes:string;Area:string;Priority:string;Status:string;due:string};
export type PhoneApp={id:string;name:string;icon:string};
export type Device={model:string;android:string;battery:number;charging:boolean;defaultHome:boolean;version:string;liveHome:boolean};

export type UpdateInfo={available:boolean;published?:boolean;version:string;versionCode:number;currentVersion:string;url?:string};
