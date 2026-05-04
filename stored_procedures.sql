-- File: stored_procedures.sql
-- Description: Transactional stored procedures for secure resource booking.

USE SmartCampusNexus;
GO

CREATE PROCEDURE sp_BookResource
    @ResID INT,
    @UserID INT,
    @Success BIT OUTPUT
AS
BEGIN
    SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
    BEGIN TRANSACTION;

    -- Check whether the resource is still available before booking it
    IF EXISTS (SELECT 1 FROM Resources WHERE ResourceID = @ResID AND Status = 'Available')
    BEGIN
        UPDATE Resources
        SET Status = 'Booked', CurrentUserID = @UserID
        WHERE ResourceID = @ResID;

        SET @Success = 1;
        COMMIT;
    END
    ELSE
    BEGIN
        SET @Success = 0;
        ROLLBACK;
    END
END;
GO
