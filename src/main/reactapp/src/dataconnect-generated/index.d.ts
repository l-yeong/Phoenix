import { ConnectorConfig, DataConnect, QueryRef, QueryPromise, MutationRef, MutationPromise } from 'firebase/data-connect';

export const connectorConfig: ConnectorConfig;

export type TimestampString = string;
export type UUIDString = string;
export type Int64String = string;
export type DateString = string;




export interface DeletePageContentData {
  pageContent_delete?: PageContent_Key | null;
}

export interface DeletePageContentVariables {
  id: UUIDString;
}

export interface GetPageContentByPathData {
  pageContents: ({
    id: UUIDString;
    contentHtml: string;
    lastUpdated: TimestampString;
    metaDescription?: string | null;
    pageTitle?: string | null;
    path: string;
    scriptsJs?: string | null;
    stylesCss?: string | null;
  } & PageContent_Key)[];
}

export interface GetPageContentByPathVariables {
  path: string;
}

export interface InsertPageContentData {
  pageContent_insert: PageContent_Key;
}

export interface InsertPageContentVariables {
  contentHtml: string;
  path: string;
  lastUpdated: TimestampString;
  metaDescription?: string | null;
  pageTitle?: string | null;
  scriptsJs?: string | null;
  stylesCss?: string | null;
}

export interface PageContent_Key {
  id: UUIDString;
  __typename?: 'PageContent_Key';
}

export interface UpdatePageContentData {
  pageContent_update?: PageContent_Key | null;
}

export interface UpdatePageContentVariables {
  id: UUIDString;
  contentHtml?: string | null;
  lastUpdated?: TimestampString | null;
  metaDescription?: string | null;
  pageTitle?: string | null;
  path?: string | null;
  scriptsJs?: string | null;
  stylesCss?: string | null;
}

interface InsertPageContentRef {
  /* Allow users to create refs without passing in DataConnect */
  (vars: InsertPageContentVariables): MutationRef<InsertPageContentData, InsertPageContentVariables>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect, vars: InsertPageContentVariables): MutationRef<InsertPageContentData, InsertPageContentVariables>;
  operationName: string;
}
export const insertPageContentRef: InsertPageContentRef;

export function insertPageContent(vars: InsertPageContentVariables): MutationPromise<InsertPageContentData, InsertPageContentVariables>;
export function insertPageContent(dc: DataConnect, vars: InsertPageContentVariables): MutationPromise<InsertPageContentData, InsertPageContentVariables>;

interface GetPageContentByPathRef {
  /* Allow users to create refs without passing in DataConnect */
  (vars: GetPageContentByPathVariables): QueryRef<GetPageContentByPathData, GetPageContentByPathVariables>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect, vars: GetPageContentByPathVariables): QueryRef<GetPageContentByPathData, GetPageContentByPathVariables>;
  operationName: string;
}
export const getPageContentByPathRef: GetPageContentByPathRef;

export function getPageContentByPath(vars: GetPageContentByPathVariables): QueryPromise<GetPageContentByPathData, GetPageContentByPathVariables>;
export function getPageContentByPath(dc: DataConnect, vars: GetPageContentByPathVariables): QueryPromise<GetPageContentByPathData, GetPageContentByPathVariables>;

interface UpdatePageContentRef {
  /* Allow users to create refs without passing in DataConnect */
  (vars: UpdatePageContentVariables): MutationRef<UpdatePageContentData, UpdatePageContentVariables>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect, vars: UpdatePageContentVariables): MutationRef<UpdatePageContentData, UpdatePageContentVariables>;
  operationName: string;
}
export const updatePageContentRef: UpdatePageContentRef;

export function updatePageContent(vars: UpdatePageContentVariables): MutationPromise<UpdatePageContentData, UpdatePageContentVariables>;
export function updatePageContent(dc: DataConnect, vars: UpdatePageContentVariables): MutationPromise<UpdatePageContentData, UpdatePageContentVariables>;

interface DeletePageContentRef {
  /* Allow users to create refs without passing in DataConnect */
  (vars: DeletePageContentVariables): MutationRef<DeletePageContentData, DeletePageContentVariables>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect, vars: DeletePageContentVariables): MutationRef<DeletePageContentData, DeletePageContentVariables>;
  operationName: string;
}
export const deletePageContentRef: DeletePageContentRef;

export function deletePageContent(vars: DeletePageContentVariables): MutationPromise<DeletePageContentData, DeletePageContentVariables>;
export function deletePageContent(dc: DataConnect, vars: DeletePageContentVariables): MutationPromise<DeletePageContentData, DeletePageContentVariables>;

