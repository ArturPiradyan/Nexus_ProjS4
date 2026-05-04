-- File: schema.sql
-- Description: Database creation and table definitions for Smart Campus Nexus.

CREATE DATABASE SmartCampusNexus;
GO
USE SmartCampusNexus;
GO

-- Physical locations on campus
CREATE TABLE MapNodes (
    NodeID NVARCHAR(50) PRIMARY KEY,
    RoomName NVARCHAR(100),
    FloorLevel INT,
    IsAccessible BIT DEFAULT 1
);

-- Paths between nodes with dynamic weights
CREATE TABLE MapEdges (
    EdgeID INT PRIMARY KEY IDENTITY(1,1),
    SourceNode NVARCHAR(50) FOREIGN KEY REFERENCES MapNodes(NodeID),
    TargetNode NVARCHAR(50) FOREIGN KEY REFERENCES MapNodes(NodeID),
    BaseDistance FLOAT,
    CongestionFactor FLOAT DEFAULT 1.0,
    IsStairs BIT DEFAULT 0
);

-- Resources located at specific nodes
CREATE TABLE Resources (
    ResourceID INT PRIMARY KEY IDENTITY(1,1),
    NodeID NVARCHAR(50) FOREIGN KEY REFERENCES MapNodes(NodeID),
    Type NVARCHAR(50), -- e.g., 'PC', 'StudyRoom'
    Status NVARCHAR(20) DEFAULT 'Available', -- 'Available', 'Booked', 'Occupied'
    CurrentUserID INT NULL
);
GO
